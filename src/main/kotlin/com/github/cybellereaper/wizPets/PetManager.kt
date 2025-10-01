package com.github.cybellereaper.wizPets

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
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
import org.bukkit.scheduler.BukkitTask
import kotlin.math.max
import kotlin.random.Random

class PetManager(private val plugin: WizPets) : Listener {
    private val pets = mutableMapOf<Player, Pet>()
    private val random = Random(System.currentTimeMillis())

    fun spawnOrRespawnPet(player: Player) {
        pets[player]?.remove()
        val spawnLocation = player.location.add(0.5, 0.0, 0.5)
        val armorStand = spawnArmorStand(spawnLocation)
        val evs = StatSet.randomEV(random)
        val ivs = StatSet.randomIV(random)
        val talents = TalentRegistry.rollTalents(random)
        val pet = Pet(plugin, player, armorStand, evs, ivs, talents)
        pet.spawn()
        pets[player] = pet
        player.sendMessage("§aYour pet answers your call.")
    }

    fun restorePet(player: Player) {
        if (pets.containsKey(player)) return
        if (!player.isOnline) return
        // Auto-summon if config says so; default true
        if (plugin.config.getBoolean("autosummon", true)) {
            spawnOrRespawnPet(player)
        }
    }

    fun dismissPet(player: Player) {
        pets.remove(player)?.remove()
        player.sendMessage("§cYour pet returns to its dormitory.")
    }

    fun removeAllPets() {
        pets.values.forEach { it.remove() }
        pets.clear()
    }

    fun getPet(player: Player): Pet? = pets[player]

    private fun spawnArmorStand(location: Location): ArmorStand {
        val stand = location.world?.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
        stand.setGravity(false)
        stand.isVisible = false
        stand.isMarker = true
        stand.equipment?.helmet = ItemStack(Material.AMETHYST_CLUSTER)
        stand.customName = "Magical Pet"
        stand.isCustomNameVisible = true
        return stand
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.getScheduler().runTask(plugin) {
            restorePet(event.player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        pets.remove(event.player)?.remove()
    }
}

class Pet(
    private val plugin: WizPets,
    val owner: Player,
    private val armorStand: ArmorStand,
    private val evs: StatSet,
    private val ivs: StatSet,
    val talents: List<Talent>
) {
    private var tickTask: BukkitTask? = null
    private var attackCooldown = 0
    val displayName: String = "${owner.name}'s Pet"

    private val baseStats = StatSet(health = 40.0, attack = 6.0, defense = 3.0, magic = 5.0)

    fun spawn() {
        armorStand.customName = displayName
        armorStand.isCustomNameVisible = true
        armorStand.isSmall = true
        armorStand.isInvisible = false
        armorStand.equipment?.helmet = ItemStack(Material.END_ROD)
        armorStand.equipment?.chestplate = ItemStack(Material.LEATHER_CHESTPLATE)
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, { tick() }, 20L, 20L)
        talents.forEach { it.onSummon(this) }
    }

    fun getStat(stat: StatType): Double {
        val base = baseStats[stat]
        val iv = ivs[stat]
        val evContribution = evs[stat] / 4.0
        return base + iv + evContribution
    }

    fun getStatBreakdown(): Map<String, Double> = StatType.entries.associate { it.displayName to getStat(it) }

    private fun tick() {
        if (!owner.isOnline || owner.isDead) {
            remove()
            return
        }

        val targetLocation = owner.location.add(0.5, 1.0, 0.5)
        armorStand.teleport(targetLocation)
        talents.forEach { it.tick(this) }
        if (attackCooldown > 0) {
            attackCooldown--
        }
        handleHealing()
        handleCombat()
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
            talents.forEach { it.onAttack(this, target, damage) }
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

    fun remove() {
        tickTask?.cancel()
        tickTask = null
        talents.forEach { it.onDismiss(this) }
        armorStand.remove()
    }
}

data class StatSet(
    val health: Double,
    val attack: Double,
    val defense: Double,
    val magic: Double
) {
    operator fun get(type: StatType): Double = when (type) {
        StatType.HEALTH -> health
        StatType.ATTACK -> attack
        StatType.DEFENSE -> defense
        StatType.MAGIC -> magic
    }

    companion object {
        fun randomEV(random: Random): StatSet = StatSet(
            health = random.nextDouble(0.0, 64.0),
            attack = random.nextDouble(0.0, 64.0),
            defense = random.nextDouble(0.0, 64.0),
            magic = random.nextDouble(0.0, 64.0)
        )

        fun randomIV(random: Random): StatSet = StatSet(
            health = random.nextDouble(0.0, 15.0),
            attack = random.nextDouble(0.0, 15.0),
            defense = random.nextDouble(0.0, 15.0),
            magic = random.nextDouble(0.0, 15.0)
        )
    }
}

enum class StatType(val displayName: String) {
    HEALTH("Health"),
    ATTACK("Attack"),
    DEFENSE("Defense"),
    MAGIC("Magic")
}
