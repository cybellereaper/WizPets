package com.github.cybellereaper.wizpets.core.service

import com.github.cybellereaper.wizpets.api.ActivePet
import com.github.cybellereaper.wizpets.api.DismissReason
import com.github.cybellereaper.wizpets.api.PetRecord
import com.github.cybellereaper.wizpets.api.StatSet
import com.github.cybellereaper.wizpets.api.StatType
import com.github.cybellereaper.wizpets.api.SummonReason
import com.github.cybellereaper.wizpets.api.WizPetsApi
import com.github.cybellereaper.wizpets.api.talent.PetTalent
import com.github.cybellereaper.wizpets.api.talent.TalentFactory
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView
import com.github.cybellereaper.wizpets.api.timeline.PetLifecycleListener
import com.github.cybellereaper.wizpets.core.config.PluginConfig
import com.github.cybellereaper.wizpets.core.persistence.PetStorage
import com.github.cybellereaper.wizpets.core.pet.ActivePetImpl
import com.github.cybellereaper.wizpets.core.talent.TalentRegistryImpl
import com.github.cybellereaper.wizpets.core.talent.defaults.ArcaneBurstTalent
import com.github.cybellereaper.wizpets.core.talent.defaults.GuardianShellTalent
import com.github.cybellereaper.wizpets.core.talent.defaults.HealingAuraTalent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.random.Random

class PetServiceImpl(val plugin: JavaPlugin) : WizPetsApi, Listener {
    val config = PluginConfig(plugin.config)
    private val storage = PetStorage(plugin)
    private val registry = TalentRegistryImpl()
    private val listeners = mutableSetOf<PetLifecycleListener>()
    private val activePets = ConcurrentHashMap<UUID, ActivePetImpl>()
    private val random = Random(System.currentTimeMillis())

    init {
        registerDefaults()
        plugin.server.servicesManager.register(WizPetsApi::class.java, this, plugin, ServicePriority.Normal)
    }

    override fun activePet(player: Player): ActivePet? = activePets[player.uniqueId]

    override fun storedPet(player: Player): PetRecord? = storage.load(player)

    override fun summon(player: Player, reason: SummonReason): ActivePet {
        val existing = activePets.remove(player.uniqueId)
        if (existing != null) {
            storage.save(player, existing.toRecord())
            existing.remove(persistFlight = true)
        }
        val loaded = storage.load(player) ?: createNewRecord(player)
        val resolved = resolveTalents(loaded)
        val pet = ActivePetImpl(this, player, resolved.first, resolved.second)
        pet.spawn()
        activePets[player.uniqueId] = pet
        storage.save(player, pet.toRecord())
        listeners.forEach { it.onSummoned(player, pet, reason) }
        plugin.logger.fine("Summoned pet for ${player.name} via $reason")
        return pet
    }

    override fun dismiss(player: Player, reason: DismissReason): Boolean = dismissInternal(player, reason, persistFlight = reason == DismissReason.PLUGIN_DISABLE)

    internal fun dismiss(player: Player, internal: Boolean = false) {
        dismissInternal(player, if (internal) DismissReason.PLAYER_QUIT else DismissReason.MANUAL, persistFlight = internal)
    }

    private fun dismissInternal(player: Player, reason: DismissReason, persistFlight: Boolean): Boolean {
        val pet = activePets.remove(player.uniqueId) ?: return false
        val record = pet.toRecord()
        storage.save(player, record)
        listeners.forEach { it.onDismissed(player, record, reason) }
        pet.remove(persistFlight)
        plugin.logger.fine("Dismissed pet for ${player.name} via $reason")
        return true
    }

    override fun persist(player: Player) {
        val pet = activePets[player.uniqueId] ?: return
        val record = pet.toRecord()
        storage.save(player, record)
        listeners.forEach { it.onPersist(player, record) }
    }

    override fun talents(): TalentRegistryView = registry

    override fun registerTalent(factory: TalentFactory, replace: Boolean) {
        registry.register(factory, replace)
        refreshActivePets()
    }

    override fun unregisterTalent(id: String) {
        registry.unregister(id)
        refreshActivePets()
    }

    override fun addListener(listener: PetLifecycleListener) {
        listeners += listener
    }

    override fun removeListener(listener: PetLifecycleListener) {
        listeners -= listener
    }

    fun mount(player: Player): Boolean {
        val pet = activePets[player.uniqueId] ?: return false
        val mounted = pet.mount()
        if (mounted) {
            persist(player)
        }
        return mounted
    }

    fun dismount(player: Player): Boolean {
        val pet = activePets[player.uniqueId] ?: return false
        val result = pet.dismount()
        if (result) {
            persist(player)
        }
        return result
    }

    fun enableFlight(player: Player): Boolean {
        val pet = activePets[player.uniqueId] ?: return false
        val started = pet.startFlying()
        if (started) {
            persist(player)
        }
        return started
    }

    fun disableFlight(player: Player): Boolean {
        val pet = activePets[player.uniqueId] ?: return false
        val stopped = pet.stopFlying()
        if (stopped) {
            persist(player)
        }
        return stopped
    }

    fun restoreOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach { player ->
            if (config.autoSummon) {
                summon(player, SummonReason.RESTORE)
            }
        }
    }

    fun shutdown() {
        activePets.values.toList().forEach { pet ->
            val owner = pet.owner
            storage.save(owner, pet.toRecord())
            pet.remove(persistFlight = false)
            listeners.forEach { it.onDismissed(owner, pet.toRecord(), DismissReason.PLUGIN_DISABLE) }
        }
        activePets.clear()
    }

    fun unregister() {
        plugin.server.servicesManager.unregister(WizPetsApi::class.java, this)
    }

    fun breed(player: Player, partner: Player) {
        val playerRecord = storage.load(player) ?: createNewRecord(player)
        val partnerRecord = storage.load(partner) ?: return

        val generation = max(playerRecord.generation, partnerRecord.generation) + 1
        val childRecord = PetRecord(
            displayName = "${player.name}'s Hatchling",
            evs = playerRecord.evs.breedWith(partnerRecord.evs, random),
            ivs = playerRecord.ivs.breedWith(partnerRecord.ivs, random),
            talentIds = registry.inherit(playerRecord.talentIds, partnerRecord.talentIds, random),
            generation = generation,
            breedCount = playerRecord.breedCount + 1,
            mountUnlocked = playerRecord.mountUnlocked || partnerRecord.mountUnlocked,
            flightUnlocked = playerRecord.flightUnlocked || partnerRecord.flightUnlocked
        )

        storage.save(player, childRecord)
        listeners.forEach { it.onPersist(player, childRecord) }
        summon(player, SummonReason.BREEDING_REFRESH)

        val updatedPartner = partnerRecord.copy(breedCount = partnerRecord.breedCount + 1)
        storage.save(partner, updatedPartner)
        listeners.forEach { it.onPersist(partner, updatedPartner) }
        activePets[partner.uniqueId]?.update(updatedPartner, resolveTalents(updatedPartner).second)
    }

    fun debugLines(player: Player): List<String> {
        val record = storage.load(player) ?: return listOf("§cNo stored pet data found.")
        val stats = StatType.entries.joinToString(", ") { "${it.displayName}: ${"%.2f".format(record.evs[it] / 4.0 + record.ivs[it])}" }
        return listOf(
            "§7==== §dPet Debug §7====",
            "§7Name: §f${record.displayName}",
            "§7Generation: §f${record.generation}  §7Breeds: §f${record.breedCount}",
            "§7Talents: §f${if (record.talentIds.isEmpty()) "None" else record.talentIds.joinToString(", ")}",
            "§7Mount unlocked: §f${record.mountUnlocked}",
            "§7Flight unlocked: §f${record.flightUnlocked}",
            "§7Base Stats: §f$stats"
        )
    }

    private fun createNewRecord(player: Player): PetRecord {
        val record = PetRecord(
            displayName = config.defaultPetName.replace("{player}", player.name),
            evs = StatSet.randomEV(random),
            ivs = StatSet.randomIV(random),
            talentIds = registry.roll(random),
            generation = 1,
            breedCount = 0,
            mountUnlocked = false,
            flightUnlocked = false
        )
        storage.save(player, record)
        return record
    }

    private fun refreshActivePets() {
        activePets.values.forEach { pet ->
            val owner = pet.owner
            val record = storage.load(owner) ?: pet.toRecord()
            val resolved = resolveTalents(record)
            storage.save(owner, resolved.first)
            pet.update(resolved.first, resolved.second)
        }
    }

    private fun registerDefaults() {
        registry.register({ HealingAuraTalent() })
        registry.register({ GuardianShellTalent() })
        registry.register({ ArcaneBurstTalent() })
    }

    private fun resolveTalents(record: PetRecord): Pair<PetRecord, List<PetTalent>> {
        var currentRecord = record
        var talents = registry.instantiate(currentRecord.talentIds)
        if (talents.size != currentRecord.talentIds.size || talents.isEmpty()) {
            val ids = registry.roll(random)
            currentRecord = currentRecord.copy(talentIds = ids)
            talents = registry.instantiate(ids)
        }
        return currentRecord to talents
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (config.autoSummon) {
            Bukkit.getScheduler().runTask(plugin, Runnable { summon(event.player, SummonReason.AUTO_SUMMON) })
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        dismissInternal(event.player, DismissReason.PLAYER_QUIT, persistFlight = false)
    }
}
