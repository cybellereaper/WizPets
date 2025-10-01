package com.github.cybellereaper.wizPets

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.logging.Level
import kotlin.math.max
import kotlin.random.Random

class PetManager(private val plugin: WizPets) : Listener {
    private val pets = mutableMapOf<Player, Pet>()
    private val random = Random(System.currentTimeMillis())
    private val storage = PetStorage(plugin)
    private val armorStandOwnerKey = NamespacedKey(plugin, "pet_owner")

    fun spawnOrRespawnPet(player: Player) {
        plugin.logger.fine("Spawning pet for ${player.name}")
        pets.remove(player)?.let {
            persistData(it)
            it.remove()
        }

        val data = storage.load(player) ?: createNewPetData(player)
        val spawnLocation = player.location.add(0.5, 0.0, 0.5)
        val armorStand = spawnArmorStand(spawnLocation, player)
        val talents = TalentRegistry.instantiate(data.talentIds)
        if (talents.size != data.talentIds.size) {
            plugin.logger.log(Level.WARNING, "One or more talents could not be instantiated for {0}: {1}", arrayOf(player.name, data.talentIds))
        }
        val pet = Pet(plugin, this, player, armorStand, data, talents)
        pet.spawn()
        pets[player] = pet
        persistData(pet)
        player.sendMessage("§aYour pet answers your call.")
        plugin.logger.log(Level.FINE, "Pet spawned for {0} with talents {1}", arrayOf(player.name, data.talentIds))
    }

    fun restorePet(player: Player) {
        if (pets.containsKey(player) || !player.isOnline) return
        if (plugin.config.getBoolean("autosummon", true)) {
            plugin.logger.fine("Restoring pet for ${player.name}")
            spawnOrRespawnPet(player)
        }
    }

    fun dismissPet(player: Player) {
        val pet = pets.remove(player)
        if (pet != null) {
            persistData(pet)
            pet.remove()
            player.sendMessage("§cYour pet returns to its dormitory.")
            plugin.logger.log(Level.INFO, "Dismissed pet for {0}", player.name)
        } else {
            player.sendMessage("You do not have an active pet.")
        }
    }

    fun removeAllPets() {
        pets.values.forEach { pet ->
            persistData(pet)
            pet.remove()
        }
        pets.clear()
        cleanupArmorStands()
    }

    fun getPet(player: Player): Pet? = pets[player]

    fun persistData(pet: Pet) {
        storage.save(pet.owner, pet.toData())
    }

    fun mountPet(player: Player) {
        val pet = getPet(player)
        if (pet == null) {
            player.sendMessage("You do not have an active pet.")
            return
        }
        if (pet.mountOwner()) {
            persistData(pet)
            plugin.logger.fine("${player.name} mounted their pet")
            player.sendMessage("§bYou climb onto your pet.")
        } else {
            player.sendMessage("§eYou are already mounted or your pet cannot carry you right now.")
        }
    }

    fun dismountPet(player: Player) {
        getPet(player)?.let {
            if (it.dismountOwner()) {
                persistData(it)
                plugin.logger.fine("${player.name} dismounted their pet")
                player.sendMessage("§7You hop off your pet.")
            } else {
                player.sendMessage("§eYou are not currently riding your pet.")
            }
        } ?: player.sendMessage("You do not have an active pet.")
    }

    fun enableFlight(player: Player) {
        val pet = getPet(player)
        if (pet == null) {
            player.sendMessage("You do not have an active pet.")
            return
        }
        if (pet.startFlying()) {
            persistData(pet)
            plugin.logger.log(Level.INFO, "{0} began flying with their pet", player.name)
            player.sendMessage("§3Wings of mana lift you skyward.")
        } else {
            player.sendMessage("§eYou are already soaring with your pet.")
        }
    }

    fun disableFlight(player: Player) {
        val pet = getPet(player)
        if (pet == null) {
            player.sendMessage("You do not have an active pet.")
            return
        }
        if (pet.stopFlying()) {
            persistData(pet)
            plugin.logger.fine("${player.name} landed with assistance from their pet")
            player.sendMessage("§7You drift back to the ground.")
        } else {
            player.sendMessage("§eYou are not currently flying with your pet.")
        }
    }

    fun breedPets(player: Player, partnerName: String) {
        val partner = Bukkit.getPlayerExact(partnerName)
        if (partner == null || !partner.isOnline) {
            player.sendMessage("§cThe specified partner is not online.")
            return
        }
        if (partner == player) {
            player.sendMessage("§cYou must choose another player to breed with.")
            return
        }

        val playerData = storage.load(player) ?: createNewPetData(player)
        val partnerData = storage.load(partner)
        if (partnerData == null) {
            player.sendMessage("§c${partner.name} does not have any pet data yet.")
            return
        }
        val generation = max(playerData.generation, partnerData.generation) + 1

        val childData = PetData(
            displayName = "${player.name}'s Hatchling", 
            evs = playerData.evs.breedWith(partnerData.evs, random),
            ivs = playerData.ivs.breedWith(partnerData.ivs, random),
            talentIds = TalentRegistry.inherit(playerData.talentIds, partnerData.talentIds, random),
            generation = generation,
            breedCount = playerData.breedCount + 1,
            mountUnlocked = playerData.mountUnlocked || partnerData.mountUnlocked,
            flightUnlocked = playerData.flightUnlocked || partnerData.flightUnlocked
        )

        storage.save(player, childData)
        spawnOrRespawnPet(player)

        val updatedPartnerData = partnerData.copy(breedCount = partnerData.breedCount + 1)
        storage.save(partner, updatedPartnerData)
        pets[partner]?.let {
            it.updateData(updatedPartnerData, TalentRegistry.instantiate(updatedPartnerData.talentIds))
            persistData(it)
            it.refreshVisuals()
        }

        player.sendMessage("§dYour pet egg hums with new potential. Generation $generation!")
        partner.sendMessage("§dYour pet assisted ${player.name} in breeding.")
        plugin.logger.log(Level.INFO, "{0} and {1} bred pets. Result talents {2}", arrayOf(player.name, partner.name, childData.talentIds))
    }

    fun showDebug(player: Player) {
        val data = storage.load(player)
        if (data == null) {
            player.sendMessage("§cNo stored pet data found.")
            return
        }
        player.sendMessage("§7==== §dPet Debug §7====")
        player.sendMessage("§7Name: §f${data.displayName}")
        player.sendMessage("§7Generation: §f${data.generation}  §7Breeds: §f${data.breedCount}")
        player.sendMessage("§7Talents: §f${if (data.talentIds.isEmpty()) "None" else data.talentIds.joinToString(", ")}")
        player.sendMessage("§7Mount unlocked: §f${data.mountUnlocked}")
        player.sendMessage("§7Flight unlocked: §f${data.flightUnlocked}")
    }

    private fun createNewPetData(player: Player): PetData {
        val evs = StatSet.randomEV(random)
        val ivs = StatSet.randomIV(random)
        val talents = TalentRegistry.rollTalentIds(random)
        val data = PetData(
            displayName = "${player.name}'s Familiar",
            evs = evs,
            ivs = ivs,
            talentIds = talents,
            generation = 1,
            breedCount = 0,
            mountUnlocked = false,
            flightUnlocked = false
        )
        storage.save(player, data)
        plugin.logger.fine("Generated new pet data for ${player.name}")
        return data
    }

    private fun spawnArmorStand(location: Location, player: Player): ArmorStand {
        val stand = location.world?.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
        stand.setGravity(false)
        stand.isVisible = false
        stand.isMarker = false
        stand.equipment?.helmet = ItemStack(Material.AMETHYST_CLUSTER)
        stand.customName = "Magical Pet"
        stand.isCustomNameVisible = true
        stand.persistentDataContainer.set(armorStandOwnerKey, PersistentDataType.STRING, player.uniqueId.toString())
        return stand
    }

    private fun cleanupArmorStands() {
        val removed = Bukkit.getWorlds().sumOf { world ->
            world.entities.filterIsInstance<ArmorStand>()
                .filter { it.persistentDataContainer.has(armorStandOwnerKey, PersistentDataType.STRING) }
                .onEach { it.remove() }
                .count()
        }
        if (removed > 0) {
            plugin.logger.log(Level.INFO, "Cleaned up {0} orphaned pet armor stands", removed)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.getScheduler().runTask(plugin) { restorePet(event.player) }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        pets.remove(event.player)?.let {
            persistData(it)
            it.remove()
            plugin.logger.log(Level.INFO, "Removed pet for {0} on disconnect", event.player.name)
        }
    }
}

class Pet(
    private val plugin: WizPets,
    private val manager: PetManager,
    val owner: Player,
    private val armorStand: ArmorStand,
    private var data: PetData,
    private var talentsInternal: List<Talent>
) {
    private var tickTask: BukkitTask? = null
    private var attackCooldown = 0
    private var isMounted = false
    private var isFlying = false
    val displayName: String
        get() = data.displayName
    val talents: List<Talent>
        get() = talentsInternal

    private val baseStats = StatSet(health = 40.0, attack = 6.0, defense = 3.0, magic = 5.0)

    fun spawn() {
        armorStand.customName = displayName
        armorStand.isCustomNameVisible = true
        armorStand.isSmall = true
        armorStand.isInvisible = false
        armorStand.equipment?.helmet = ItemStack(Material.END_ROD)
        armorStand.equipment?.chestplate = ItemStack(Material.LEATHER_CHESTPLATE)
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, { tick() }, 20L, 20L)
        talentsInternal.forEach { it.onSummon(this) }
        if (data.flightUnlocked) {
            owner.allowFlight = true
        }
    }

    fun updateData(newData: PetData, newTalents: List<Talent>) {
        data = newData
        talentsInternal = newTalents
        armorStand.customName = displayName
    }

    fun refreshVisuals() {
        armorStand.isCustomNameVisible = true
        armorStand.customName = displayName
    }

    fun toData(): PetData = data

    fun getStat(stat: StatType): Double {
        val base = baseStats[stat]
        val iv = data.ivs[stat]
        val evContribution = data.evs[stat] / 4.0
        return base + iv + evContribution
    }

    fun getStatBreakdown(): Map<String, Double> = StatType.entries.associate { it.displayName to getStat(it) }

    private fun tick() {
        if (!owner.isOnline || owner.isDead) {
            manager.persistData(this)
            remove()
            return
        }

        val targetLocation = if (isMounted) owner.location else owner.location.add(0.5, 1.0, 0.5)
        armorStand.teleport(targetLocation)
        talentsInternal.forEach { it.tick(this) }
        if (attackCooldown > 0) {
            attackCooldown--
        }
        handleHealing()
        handleCombat()
        if (isFlying) {
            owner.fallDistance = 0f
        }
    }

    private fun handleHealing() {
        val ownerHealth = owner.health
        val maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        if (ownerHealth < maxHealth) {
            val healAmount = max(0.5, getStat(StatType.MAGIC) * 0.05)
            owner.health = minOf(ownerHealth + healAmount, maxHealth)
        }
    }

    private fun handleCombat() {
        val world = armorStand.world
        val nearbyMonsters = world.getNearbyEntities(armorStand.location, 8.0, 6.0, 8.0)
            .filterIsInstance<Monster>()
            .filter { it.isValid && !it.isDead }
        val target = nearbyMonsters.minByOrNull { it.location.distanceSquared(armorStand.location) }
        if (target != null && attackCooldown <= 0) {
            val damage = getStat(StatType.ATTACK) * 0.75 + getStat(StatType.MAGIC) * 0.25
            target.damage(damage, owner)
            owner.sendActionBar(Component.text("§d${displayName} hits ${target.name} for ${"%.1f".format(damage)}"))
            talentsInternal.forEach { it.onAttack(this, target, damage) }
            attackCooldown = max(2, (6 - getStat(StatType.MAGIC) / 4).toInt())
        }
    }

    fun heal(amount: Double) {
        val maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        owner.health = minOf(owner.health + amount, maxHealth)
    }

    fun grantAbsorption(hearts: Double) {
        val current = owner.absorptionAmount
        owner.absorptionAmount = current.coerceAtLeast(hearts)
    }

    fun mountOwner(): Boolean {
        if (!data.mountUnlocked) {
            data = data.copy(mountUnlocked = true)
            owner.sendMessage("§bYour bond deepens, allowing you to ride your pet!")
        }
        if (owner.vehicle == armorStand) {
            return false
        }
        val mounted = armorStand.addPassenger(owner)
        if (mounted) {
            isMounted = true
        }
        return mounted
    }

    fun dismountOwner(): Boolean {
        if (owner.vehicle == armorStand) {
            owner.leaveVehicle()
            isMounted = false
            return true
        }
        return false
    }

    fun startFlying(): Boolean {
        if (!data.flightUnlocked) {
            data = data.copy(flightUnlocked = true)
            owner.sendMessage("§3Your pet lifts you into the skies!")
        }
        if (owner.allowFlight && owner.isFlying) {
            return false
        }
        owner.allowFlight = true
        owner.isFlying = true
        isFlying = true
        return true
    }

    fun stopFlying(): Boolean {
        if (!isFlying) return false
        isFlying = false
        if (owner.gameMode != GameMode.CREATIVE && owner.gameMode != GameMode.SPECTATOR) {
            owner.isFlying = false
            if (!data.flightUnlocked) {
                owner.allowFlight = false
            }
        }
        return true
    }

    fun remove() {
        tickTask?.cancel()
        tickTask = null
        talentsInternal.forEach { it.onDismiss(this) }
        if (owner.vehicle == armorStand) {
            owner.leaveVehicle()
        }
        if (owner.gameMode != GameMode.CREATIVE && owner.gameMode != GameMode.SPECTATOR && !data.flightUnlocked) {
            owner.allowFlight = false
        }
        armorStand.remove()
    }
}
