package com.github.cybellereaper.wizPets

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Ageable
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.logging.Level
import kotlin.math.max
import kotlin.random.Random

class PetManager(private val plugin: WizPets) : Listener {
    private val pets = mutableMapOf<Player, Pet>()
    private val random = Random(System.currentTimeMillis())
    private val storage = PetStorage(plugin)
    private val entityOwnerKey = NamespacedKey(plugin, "pet_owner")
    private val entityScriptKey = NamespacedKey(plugin, "pet_script")

    fun spawnOrRespawnPet(player: Player) {
        plugin.logger.fine("Spawning pet for ${player.name}")
        pets.remove(player)?.let {
            persistData(it)
            it.remove()
        }

        val data = storage.load(player) ?: createNewPetData(player)
        val definition = plugin.scriptManager.definitionFor(data.scriptId)
        val spawnLocation = player.location.add(0.5, 0.0, 0.5)
        val mob = spawnPetEntity(player, spawnLocation, definition)
        val talents = TalentRegistry.instantiate(data.talentIds)
        if (talents.size != data.talentIds.size) {
            plugin.logger.log(
                Level.WARNING,
                "One or more talents could not be instantiated for {0}: {1}",
                arrayOf(player.name, data.talentIds)
            )
        }
        val pet = Pet(plugin, this, player, mob, data, talents, definition)
        pet.spawn()
        pets[player] = pet
        persistData(pet)
        player.sendMessage("§aYour pet answers your call.")
        plugin.logger.log(Level.FINE, "Pet spawned for {0} using script {1}", arrayOf(player.name, definition.id))
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
        if (!pet.canMount()) {
            player.sendMessage("§eThis pet cannot be mounted right now.")
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
        if (!pet.canFly()) {
            player.sendMessage("§eThis pet cannot fly.")
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
            flightUnlocked = playerData.flightUnlocked || partnerData.flightUnlocked,
            scriptId = playerData.scriptId
        )

        storage.save(player, childData)
        spawnOrRespawnPet(player)

        val updatedPartnerData = partnerData.copy(breedCount = partnerData.breedCount + 1)
        storage.save(partner, updatedPartnerData)
        pets[partner]?.let {
            val partnerDefinition = plugin.scriptManager.definitionFor(updatedPartnerData.scriptId)
            it.updateData(updatedPartnerData, TalentRegistry.instantiate(updatedPartnerData.talentIds), partnerDefinition)
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
        player.sendMessage("§7Script id: §f${data.scriptId}")
    }

    fun availableScripts(): List<String> = plugin.scriptManager.availableScripts.sortedBy { it.lowercase() }

    fun assignScript(player: Player, scriptName: String) {
        val definition = plugin.scriptManager.findDefinition(scriptName)
        if (definition == null) {
            player.sendMessage("§cNo script named '$scriptName' exists. Use /wizpet script list.")
            return
        }
        val data = storage.load(player) ?: createNewPetData(player)
        if (data.scriptId.equals(definition.id, ignoreCase = true)) {
            player.sendMessage("§eYour pet already uses the ${definition.id} script.")
            return
        }
        val updated = data.copy(scriptId = definition.id)
        storage.save(player, updated)
        pets[player]?.let { pet ->
            pet.updateData(updated, TalentRegistry.instantiate(updated.talentIds), definition)
            persistData(pet)
            pet.refreshVisuals()
        }
        player.sendMessage("§aYour pet now follows the §f${definition.id} §ascript.")
        plugin.logger.log(Level.INFO, "{0} assigned pet script {1}", arrayOf(player.name, definition.id))
    }

    fun reloadScripts(requester: CommandSender) {
        val loaded = plugin.scriptManager.reloadScripts()
        pets.values.forEach { pet ->
            val definition = plugin.scriptManager.definitionFor(pet.toData().scriptId)
            pet.updateDefinition(definition)
        }
        requester.sendMessage("§aReloaded $loaded pet scripts.")
        plugin.logger.log(Level.INFO, "Pet scripts reloaded by {0}", requester.name)
    }

    private fun createNewPetData(player: Player): PetData {
        val defaultDefinition = plugin.scriptManager.definitionFor(plugin.scriptManager.defaultDefinitionId)
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
            mountUnlocked = defaultDefinition.entityConfig.mountAllowed,
            flightUnlocked = defaultDefinition.entityConfig.flightAllowed,
            scriptId = defaultDefinition.id
        )
        storage.save(player, data)
        plugin.logger.fine("Generated new pet data for ${player.name}")
        return data
    }

    private fun spawnPetEntity(owner: Player, location: Location, definition: ScriptedPetDefinition): Mob {
        val world = location.world ?: throw IllegalStateException("Cannot spawn pet without a world")
        val entityType = definition.entityConfig.entityType.takeIf { type ->
            val clazz = type.entityClass
            clazz != null && Mob::class.java.isAssignableFrom(clazz)
        } ?: run {
            plugin.logger.log(Level.WARNING, "Pet entity type {0} is not a mob; falling back to WOLF", definition.entityConfig.entityType)
            EntityType.WOLF
        }
        val mob = world.spawnEntity(location, entityType) as Mob
        mob.isPersistent = false
        mob.removeWhenFarAway = false
        mob.setAI(true)
        mob.isSilent = definition.entityConfig.silent
        mob.isInvisible = definition.entityConfig.invisible
        mob.isCollidable = false
        mob.isInvulnerable = true
        mob.persistentDataContainer.set(entityOwnerKey, PersistentDataType.STRING, owner.uniqueId.toString())
        mob.persistentDataContainer.set(entityScriptKey, PersistentDataType.STRING, definition.id)
        return mob
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
    val mob: Mob,
    private var data: PetData,
    private var talentsInternal: List<Talent>,
    private var definition: ScriptedPetDefinition
) {
    private var tickTask: BukkitTask? = null
    private var isMounted = false
    private var isFlying = false
    private val scriptContext = PetScriptContext(plugin, this, plugin.particleController)
    private val baseStats: StatSet
        get() = definition.baseStats
    private val followConfig: PetEntityConfig
        get() = definition.entityConfig
    private val activeMoves = definition.moves.map { ActiveMove(it) }.toMutableList()

    val displayName: String
        get() = data.displayName
    val talents: List<Talent>
        get() = talentsInternal
    val location: Location
        get() = mob.location.clone()

    internal val currentDefinition: ScriptedPetDefinition
        get() = definition

    fun spawn() {
        configureEntity()
        updateDisplayName(definition.displayNameProvider(scriptContext))
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, { tick() }, 20L, 20L)
        talentsInternal.forEach { it.onSummon(this) }
        definition.behavior.onSummon(scriptContext)
        if (data.flightUnlocked && definition.entityConfig.flightAllowed) {
            owner.allowFlight = true
        }
    }

    fun updateData(newData: PetData, newTalents: List<Talent>, newDefinition: ScriptedPetDefinition? = null) {
        data = newData
        talentsInternal = newTalents
        newDefinition?.let { updateDefinition(it) } ?: refreshVisuals()
    }

    fun updateDefinition(newDefinition: ScriptedPetDefinition) {
        definition = newDefinition
        activeMoves.clear()
        activeMoves += definition.moves.map { ActiveMove(it) }
        configureEntity()
        updateDisplayName(definition.displayNameProvider(scriptContext))
        if (data.flightUnlocked && definition.entityConfig.flightAllowed) {
            owner.allowFlight = true
        }
        if (!definition.entityConfig.mountAllowed && owner.vehicle == mob) {
            owner.leaveVehicle()
            isMounted = false
        }
        if (!definition.entityConfig.flightAllowed) {
            if (owner.gameMode != GameMode.CREATIVE && owner.gameMode != GameMode.SPECTATOR) {
                owner.isFlying = false
                owner.allowFlight = false
            }
            isFlying = false
        }
        definition.behavior.onSummon(scriptContext)
    }

    fun refreshVisuals() {
        configureEntity()
        mob.customName = data.displayName
        mob.isCustomNameVisible = true
    }

    fun toData(): PetData = data

    fun getStat(stat: StatType): Double {
        val base = baseStats[stat]
        val iv = data.ivs[stat]
        val evContribution = data.evs[stat] / 4.0
        return base + iv + evContribution
    }

    fun getStatBreakdown(): Map<String, Double> = StatType.entries.associate { it.displayName to getStat(it) }

    fun performAttack(target: LivingEntity, damage: Double) {
        target.damage(damage, owner)
        owner.sendActionBar(Component.text("§d${displayName} hits ${target.name} for ${"%.1f".format(damage)}"))
        talentsInternal.forEach { it.onAttack(this, target, damage) }
        definition.behavior.onAttack(scriptContext, target, damage)
    }

    fun findNearestEnemy(radius: Double): LivingEntity? {
        val world = mob.world
        return world.getNearbyEntities(mob.location, radius, radius, radius)
            .filterIsInstance<Monster>()
            .filter { it.isValid && !it.isDead }
            .minByOrNull { it.location.distanceSquared(mob.location) }
    }

    fun navigateTo(location: Location, speed: Double) {
        mob.pathfinder.moveTo(location, speed)
    }

    fun stopNavigation() {
        mob.pathfinder.stopPathfinding()
    }

    fun canMount(): Boolean = definition.entityConfig.mountAllowed && data.mountUnlocked

    fun canFly(): Boolean = definition.entityConfig.flightAllowed && data.flightUnlocked

    fun mountOwner(): Boolean {
        if (!canMount()) {
            if (!data.mountUnlocked && definition.entityConfig.mountAllowed) {
                data = data.copy(mountUnlocked = true)
                owner.sendMessage("§bYour bond deepens, allowing you to ride your pet!")
            } else {
                return false
            }
        }
        if (owner.vehicle == mob) {
            return false
        }
        val mounted = mob.addPassenger(owner)
        if (mounted) {
            isMounted = true
        }
        return mounted
    }

    fun dismountOwner(): Boolean {
        if (owner.vehicle == mob) {
            owner.leaveVehicle()
            isMounted = false
            return true
        }
        return false
    }

    fun startFlying(): Boolean {
        if (!definition.entityConfig.flightAllowed) {
            return false
        }
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

    fun heal(amount: Double) {
        val maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        owner.health = minOf(owner.health + amount, maxHealth)
    }

    fun grantAbsorption(hearts: Double) {
        val current = owner.absorptionAmount
        owner.absorptionAmount = current.coerceAtLeast(hearts)
    }

    fun debug(message: String) {
        if (owner.isOnline) {
            owner.sendMessage("§8[§dPet§8] §7$message")
        }
        plugin.logger.log(Level.FINE, "[{0}] {1}", arrayOf(owner.name, message))
    }

    internal fun handleRaycastHit(entity: LivingEntity) {
        definition.behavior.onRaycastHit(scriptContext, entity)
    }

    internal fun handleAreaHit(entity: LivingEntity) {
        definition.behavior.onAreaHit(scriptContext, entity)
    }

    fun updateDisplayName(name: String) {
        data = data.copy(displayName = name)
        mob.customName = name
        mob.isCustomNameVisible = true
        manager.persistData(this)
    }

    fun remove() {
        tickTask?.cancel()
        tickTask = null
        talentsInternal.forEach { it.onDismiss(this) }
        definition.behavior.onDismiss(scriptContext)
        if (owner.vehicle == mob) {
            owner.leaveVehicle()
        }
        if (owner.gameMode != GameMode.CREATIVE && owner.gameMode != GameMode.SPECTATOR && !data.flightUnlocked) {
            owner.allowFlight = false
        }
        mob.remove()
    }

    private fun configureEntity() {
        mob.customName = data.displayName
        mob.isCustomNameVisible = true
        mob.isSilent = definition.entityConfig.silent
        mob.isInvisible = definition.entityConfig.invisible
        mob.isCollidable = false
        mob.setAI(true)
        mob.isInvulnerable = true
        if (mob is Ageable) {
            mob.setAgeLock(true)
            if (definition.entityConfig.baby) {
                mob.setBaby()
            } else {
                mob.setAdult()
            }
        }
    }

    private fun tick() {
        if (!owner.isOnline || owner.isDead || !mob.isValid) {
            manager.persistData(this)
            remove()
            return
        }

        val ownerLocation = owner.location
        val distanceSquared = mob.location.distanceSquared(ownerLocation)
        if (distanceSquared > followConfig.leashDistance * followConfig.leashDistance) {
            mob.teleport(ownerLocation)
        } else if (!isMounted) {
            if (distanceSquared > followConfig.followStartDistance * followConfig.followStartDistance) {
                navigateTo(ownerLocation, followConfig.followSpeed)
            } else if (distanceSquared < followConfig.followStopDistance * followConfig.followStopDistance) {
                stopNavigation()
            }
        }

        talentsInternal.forEach { it.tick(this) }
        handleHealing()
        if (isFlying) {
            owner.fallDistance = 0f
        }
        tickMoves()
        definition.behavior.onTick(scriptContext)
    }

    private fun handleHealing() {
        val ownerHealth = owner.health
        val maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        if (ownerHealth < maxHealth) {
            val healAmount = max(0.5, getStat(StatType.MAGIC) * 0.05)
            owner.health = minOf(ownerHealth + healAmount, maxHealth)
        }
    }

    private fun tickMoves() {
        for (active in activeMoves) {
            active.tick(scriptContext)
        }
    }

    private inner class ActiveMove(private val definition: ScriptedPetMove) {
        private var cooldown = 0

        fun tick(context: PetScriptContext) {
            if (cooldown > 0) {
                cooldown--
                return
            }
            val target = definition.selectTarget(context) ?: return
            if (!target.isValid || target.isDead) return
            val rangeSquared = definition.range * definition.range
            if (target.location.world != mob.world || target.location.distanceSquared(mob.location) > rangeSquared) {
                return
            }
            definition.execute(context, target)
            cooldown = definition.cooldownTicks
        }
    }
}
