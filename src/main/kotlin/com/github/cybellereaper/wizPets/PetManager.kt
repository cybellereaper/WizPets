package com.github.cybellereaper.wizPets

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class PetManager(private val plugin: WizPets) : Listener {
    private val random = Random(System.currentTimeMillis())
    private val profileStore = ProfileStore(plugin)
    private val profiles = profileStore.loadProfiles()
    private val activePets = mutableMapOf<Player, Pet>()

    fun capturePet(player: Player, speciesId: String, nickname: String?): CapturedPet? {
        val species = PetSpeciesRegistry.getSpecies(speciesId)
        if (species == null) {
            player.sendMessage("§cUnknown species '$speciesId'.")
            return null
        }

        val profile = getProfile(player.uniqueId)
        val resolvedNickname = resolveNickname(profile, nickname ?: species.displayName)
        val evs = StatSet.randomEV(random)
        val ivs = StatSet.randomIV(random)
        val level = plugin.config.getInt("starting-level", 5)
        val talents = TalentRegistry.rollTalents(random, species, count = 2)
        val captured = CapturedPet(
            id = UUID.randomUUID(),
            speciesId = species.id,
            nickname = resolvedNickname,
            level = level,
            experience = 0,
            evs = evs,
            ivs = ivs,
            talentIds = talents.toMutableList()
        )
        profile.pets[captured.id] = captured
        if (profile.activePetId == null) {
            profile.activePetId = captured.id
            spawnActivePet(player)
        }
        saveProfiles()
        player.sendMessage("§aYou befriended §b${resolvedNickname}§a the §d${species.displayName}§a!")
        return captured
    }

    fun spawnActivePet(player: Player) {
        val profile = getProfile(player.uniqueId)
        val captured = profile.getActivePet() ?: run {
            player.sendMessage("§7You do not have an active pet selected.")
            return
        }
        activePets[player]?.remove()
        val species = PetSpeciesRegistry.getSpecies(captured.speciesId)
        if (species == null) {
            player.sendMessage("§c${captured.nickname} cannot be summoned because its species is missing.")
            return
        }
        val armorStand = spawnArmorStand(player.location.add(0.5, 0.0, 0.5), species)
        val pet = Pet(plugin, player, captured, species, armorStand, random)
        pet.spawn()
        activePets[player] = pet
        player.sendMessage("§a${captured.nickname} appears at your side.")
    }

    fun dismissPet(player: Player) {
        activePets.remove(player)?.remove()
        player.sendMessage("§cYour pet returns to its dormitory.")
    }

    fun listPets(player: Player): List<CapturedPet> = getProfile(player.uniqueId).pets.values.sortedBy { it.nickname.lowercase() }

    fun selectPet(player: Player, nickname: String): Boolean {
        val profile = getProfile(player.uniqueId)
        val captured = profile.findByNickname(nickname)
        if (captured == null) {
            player.sendMessage("§cYou do not own a pet named $nickname.")
            return false
        }
        profile.activePetId = captured.id
        spawnActivePet(player)
        saveProfiles()
        return true
    }

    fun releasePet(player: Player, nickname: String): Boolean {
        val profile = getProfile(player.uniqueId)
        val captured = profile.findByNickname(nickname)
        if (captured == null) {
            player.sendMessage("§cYou do not own a pet named $nickname.")
            return false
        }
        profile.pets.remove(captured.id)
        if (profile.activePetId == captured.id) {
            dismissPet(player)
            profile.activePetId = profile.pets.keys.firstOrNull()
            profile.activePetId?.let { spawnActivePet(player) }
        }
        saveProfiles()
        player.sendMessage("§7You released $nickname back into the Spiral.")
        return true
    }

    fun renamePet(player: Player, oldNickname: String, newNickname: String): Boolean {
        val profile = getProfile(player.uniqueId)
        val captured = profile.findByNickname(oldNickname)
        if (captured == null) {
            player.sendMessage("§cYou do not own a pet named $oldNickname.")
            return false
        }
        val resolved = resolveNickname(profile, newNickname)
        captured.nickname = resolved
        if (profile.activePetId == captured.id) {
            activePets[player]?.refreshDisplayName()
        }
        saveProfiles()
        player.sendMessage("§aYour pet will now be known as §b$resolved§a.")
        return true
    }

    fun getActivePet(player: Player): Pet? = activePets[player]

    fun getProfile(uuid: UUID): TamerProfile = profiles.computeIfAbsent(uuid) { TamerProfile(mutableMapOf(), null) }

    fun removeAllPets() {
        activePets.values.forEach { it.remove() }
        activePets.clear()
        saveProfiles()
    }

    fun shutdown() {
        activePets.values.forEach { it.remove() }
        activePets.clear()
        profileStore.saveProfiles(profiles)
    }

    fun handleJoin(player: Player) {
        val profile = getProfile(player.uniqueId)
        if (plugin.config.getBoolean("autosummon", true) && profile.activePetId != null) {
            Bukkit.getScheduler().runTask(plugin) { spawnActivePet(player) }
        }
    }

    fun handleQuit(player: Player) {
        activePets.remove(player)?.remove()
    }

    fun adminGivePet(target: Player, speciesId: String, nickname: String?): CapturedPet? = capturePet(target, speciesId, nickname)

    fun reloadSpecies() {
        PetSpeciesRegistry.reload(plugin)
        activePets.values.forEach { it.remove() }
        activePets.keys.toList().forEach { spawnActivePet(it) }
    }

    private fun resolveNickname(profile: TamerProfile, candidate: String): String {
        val baseName = candidate.trim().ifEmpty { "Companion" }
        var name = baseName
        var index = 2
        while (profile.pets.values.any { it.nickname.equals(name, ignoreCase = true) }) {
            name = "$baseName $index"
            index++
        }
        return name
    }

    private fun spawnArmorStand(location: Location, species: PetSpecies): ArmorStand {
        val stand = location.world?.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
        stand.setGravity(false)
        stand.isVisible = false
        stand.isMarker = true
        stand.isSmall = true
        stand.equipment?.helmet = ItemStack(species.modelItem)
        stand.customName = species.displayName
        stand.isCustomNameVisible = true
        return stand
    }

    private fun saveProfiles() {
        profileStore.saveProfiles(profiles)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        handleJoin(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        handleQuit(event.player)
    }
}

class Pet(
    private val plugin: WizPets,
    val owner: Player,
    private val captured: CapturedPet,
    val species: PetSpecies,
    private val armorStand: ArmorStand,
    private val random: Random
) {
    private var tickTask: BukkitTask? = null
    private var attackCooldown = 0
    private val talents: List<Talent> = TalentRegistry.instantiate(captured.talentIds, random)
    val displayName: String
        get() = "${owner.name}'s ${captured.nickname}"
    val element: Element
        get() = species.element
    var extraCollectionRange: Double = 0.0

    fun spawn() {
        armorStand.customName = displayName
        armorStand.isCustomNameVisible = true
        armorStand.isInvisible = false
        armorStand.equipment?.helmet = ItemStack(species.modelItem)
        talents.forEach { it.onSummon(this) }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, { tick() }, 20L, 20L)
    }

    fun refreshDisplayName() {
        armorStand.customName = displayName
    }

    fun getStat(stat: StatType): Double {
        val base = species.baseStats[stat]
        val iv = captured.ivs[stat]
        val evContribution = captured.evs[stat] / 32.0
        val levelBonus = captured.level * when (stat) {
            StatType.HEALTH -> 0.9
            StatType.ATTACK -> 0.6
            StatType.DEFENSE -> 0.55
            StatType.MAGIC -> 0.65
            StatType.SPEED -> 0.7
        }
        return base + iv + evContribution + levelBonus
    }

    fun getStatBreakdown(): Map<String, Double> = StatType.entries.associate { it.displayName to getStat(it) }

    private fun tick() {
        if (!owner.isOnline || owner.isDead) {
            remove()
            return
        }

        val targetLocation = owner.location.clone().add(0.5, 1.0, 0.5)
        armorStand.teleport(targetLocation)
        talents.forEach { it.tick(this) }
        if (attackCooldown > 0) {
            attackCooldown--
        }
        handleHealing()
        handleCombat()
        handleCollection()
    }

    private fun handleHealing() {
        val maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        val healAmount = max(0.0, getStat(StatType.MAGIC) * 0.03)
        val newHealth = min(maxHealth, owner.health + healAmount)
        owner.health = newHealth
    }

    private fun handleCombat() {
        val world = armorStand.world
        val range = 10.0
        val nearby = world.getNearbyEntities(armorStand.location, range, range, range)
            .filterIsInstance<Monster>()
            .filter { it.isValid && !it.isDead }
        val target = nearby.minByOrNull { it.location.distanceSquared(armorStand.location) }
        if (target != null && attackCooldown <= 0) {
            val physical = getStat(StatType.ATTACK)
            val magical = getStat(StatType.MAGIC)
            val damage = physical * 0.6 + magical * 0.5
            target.damage(damage, owner)
            owner.sendActionBar(Component.text("§d${captured.nickname} strikes ${target.name} for ${"%.1f".format(damage)}"))
            talents.forEach { it.onAttack(this, target, damage) }
            val speedBonus = getStat(StatType.SPEED)
            attackCooldown = max(2, (6 - speedBonus / 5).toInt())
        }
    }

    private fun handleCollection() {
        val range = 2.5 + extraCollectionRange
        val items = armorStand.world.getNearbyEntities(armorStand.location, range, range, range)
            .filterIsInstance<Item>()
        for (item in items) {
            if (item.isDead || item.itemStack.type.isAir) continue
            val leftovers = owner.inventory.addItem(item.itemStack)
            if (leftovers.isEmpty()) {
                item.remove()
            } else {
                item.itemStack = leftovers.values.first()
            }
        }
    }

    fun heal(amount: Double) {
        val maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        owner.health = min(owner.health + amount, maxHealth)
    }

    fun grantAbsorption(hearts: Double) {
        owner.absorptionAmount = max(owner.absorptionAmount, hearts * 2)
    }

    fun remove() {
        tickTask?.cancel()
        tickTask = null
        talents.forEach { it.onDismiss(this) }
        armorStand.remove()
    }
}

data class CapturedPet(
    val id: UUID,
    val speciesId: String,
    var nickname: String,
    var level: Int,
    var experience: Int,
    val evs: StatSet,
    val ivs: StatSet,
    val talentIds: MutableList<String>
)

data class TamerProfile(
    val pets: MutableMap<UUID, CapturedPet>,
    var activePetId: UUID?
) {
    fun getActivePet(): CapturedPet? = activePetId?.let { pets[it] }

    fun findByNickname(nickname: String): CapturedPet? = pets.values.firstOrNull { it.nickname.equals(nickname, ignoreCase = true) }
}

private class ProfileStore(private val plugin: WizPets) {
    private val file: File = File(plugin.dataFolder, "profiles.yml")

    fun loadProfiles(): MutableMap<UUID, TamerProfile> {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val config = YamlConfiguration.loadConfiguration(file)
        val profilesSection = config.getConfigurationSection("profiles") ?: return mutableMapOf()
        val map = mutableMapOf<UUID, TamerProfile>()
        for (uuidKey in profilesSection.getKeys(false)) {
            val uuid = runCatching { UUID.fromString(uuidKey) }.getOrNull() ?: continue
            val profileSection = profilesSection.getConfigurationSection(uuidKey) ?: continue
            val petsSection = profileSection.getConfigurationSection("pets")
            val pets = mutableMapOf<UUID, CapturedPet>()
            petsSection?.getKeys(false)?.forEach { petKey ->
                val petId = runCatching { UUID.fromString(petKey) }.getOrNull() ?: return@forEach
                val petSection = petsSection.getConfigurationSection(petKey) ?: return@forEach
                val speciesId = petSection.getString("species") ?: return@forEach
                val nickname = petSection.getString("nickname") ?: speciesId
                val level = petSection.getInt("level", 5)
                val experience = petSection.getInt("experience", 0)
                val evs = petSection.parseStatSet("evs") ?: StatSet.randomEV(Random(System.nanoTime()))
                val ivs = petSection.parseStatSet("ivs") ?: StatSet.randomIV(Random(System.nanoTime()))
                val talents = petSection.getStringList("talents").map { it.lowercase() }.toMutableList()
                pets[petId] = CapturedPet(petId, speciesId, nickname, level, experience, evs, ivs, talents)
            }
            val activePet = profileSection.getString("active")?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            map[uuid] = TamerProfile(pets, activePet)
        }
        return map
    }

    fun saveProfiles(profiles: Map<UUID, TamerProfile>) {
        val config = YamlConfiguration()
        val root = config.createSection("profiles")
        for ((uuid, profile) in profiles) {
            val profileSection = root.createSection(uuid.toString())
            profile.activePetId?.let { profileSection.set("active", it.toString()) }
            val petsSection = profileSection.createSection("pets")
            for ((petId, pet) in profile.pets) {
                val petSection = petsSection.createSection(petId.toString())
                petSection.set("species", pet.speciesId)
                petSection.set("nickname", pet.nickname)
                petSection.set("level", pet.level)
                petSection.set("experience", pet.experience)
                petSection.set("evs", pet.evs.toMap())
                petSection.set("ivs", pet.ivs.toMap())
                petSection.set("talents", pet.talentIds)
            }
        }
        file.parentFile.mkdirs()
        config.save(file)
    }
}

private fun ConfigurationSection.parseStatSet(path: String): StatSet? {
    val section = getConfigurationSection(path) ?: return null
    return StatSet(
        health = section.getDouble("health", 0.0),
        attack = section.getDouble("attack", 0.0),
        defense = section.getDouble("defense", 0.0),
        magic = section.getDouble("magic", 0.0),
        speed = section.getDouble("speed", 0.0)
    )
}

private fun StatSet.toMap(): Map<String, Double> = mapOf(
    "health" to health,
    "attack" to attack,
    "defense" to defense,
    "magic" to magic,
    "speed" to speed
)
