package com.github.cybellereaper.wizpets.core.pet

import com.github.cybellereaper.wizpets.api.ActivePet
import com.github.cybellereaper.wizpets.api.PetRecord
import com.github.cybellereaper.wizpets.api.StatSet
import com.github.cybellereaper.wizpets.api.StatType
import com.github.cybellereaper.wizpets.api.talent.PetTalent
import com.github.cybellereaper.wizpets.core.service.PetServiceImpl
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import kotlin.math.max

class ActivePetImpl(
    private val service: PetServiceImpl,
    override val owner: Player,
    initialRecord: PetRecord,
    initialTalents: List<PetTalent>
) : ActivePet {
    private val plugin = service.plugin
    private val baseStats = StatSet(health = 40.0, attack = 6.0, defense = 3.0, magic = 5.0)

    private var armorStand: ArmorStand? = null
    private var tickTask: BukkitTask? = null
    private var attackCooldown = 0
    private var currentRecord: PetRecord = initialRecord
    private var currentTalents: List<PetTalent> = initialTalents
    private var mounted = false
    private var flying = false

    override val record: PetRecord get() = currentRecord
    override val talents: List<PetTalent> get() = currentTalents
    override val isMounted: Boolean get() = mounted
    override val isFlying: Boolean get() = flying

    fun spawn(location: Location = owner.location.add(0.5, 0.0, 0.5)) {
        val world = location.world ?: return
        val stand = world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
        stand.setGravity(false)
        stand.isVisible = false
        stand.isMarker = false
        stand.isSmall = true
        stand.equipment?.helmet = ItemStack(Material.END_ROD)
        stand.customName(Component.text(record.displayName))
        stand.isCustomNameVisible = true
        armorStand = stand

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, ::tick, 10L, 10L)
        currentTalents.forEach { it.onSummon(this) }
        if (record.flightUnlocked) {
            owner.allowFlight = true
        }
    }

    fun update(record: PetRecord, talents: List<PetTalent>) {
        currentRecord = record
        currentTalents = talents
        armorStand?.customName(Component.text(record.displayName))
        armorStand?.isCustomNameVisible = true
    }

    fun dismountIfNecessary() {
        if (owner.vehicle == armorStand) {
            owner.leaveVehicle()
        }
        mounted = false
    }

    fun remove(persistFlight: Boolean) {
        tickTask?.cancel()
        tickTask = null
        currentTalents.forEach { it.onDismiss(this) }
        dismountIfNecessary()
        if (!persistFlight && owner.gameMode !in listOf(GameMode.CREATIVE, GameMode.SPECTATOR)) {
            owner.allowFlight = record.flightUnlocked
            if (!record.flightUnlocked) {
                owner.isFlying = false
            }
        }
        armorStand?.remove()
        armorStand = null
    }

    fun mount(): Boolean {
        val stand = armorStand ?: return false
        if (!record.mountUnlocked) {
            currentRecord = currentRecord.copy(mountUnlocked = true)
            owner.sendMessage(service.config.mountUnlockMessage)
        }
        if (owner.vehicle == stand) return false
        val added = stand.addPassenger(owner)
        if (added) {
            mounted = true
        }
        return added
    }

    fun dismount(): Boolean {
        if (owner.vehicle != armorStand) return false
        owner.leaveVehicle()
        mounted = false
        return true
    }

    fun startFlying(): Boolean {
        if (!record.flightUnlocked) {
            currentRecord = currentRecord.copy(flightUnlocked = true)
            owner.sendMessage(service.config.flightUnlockMessage)
        }
        if (owner.allowFlight && owner.isFlying) {
            flying = true
            return false
        }
        owner.allowFlight = true
        owner.isFlying = true
        flying = true
        return true
    }

    fun stopFlying(): Boolean {
        if (!flying) return false
        flying = false
        if (owner.gameMode !in listOf(GameMode.CREATIVE, GameMode.SPECTATOR) && !record.flightUnlocked) {
            owner.isFlying = false
            owner.allowFlight = false
        }
        return true
    }

    override fun heal(amount: Double) {
        val maxHealth = owner.maxHealth
        owner.health = (owner.health + amount).coerceAtMost(maxHealth)
    }

    override fun grantAbsorption(hearts: Double) {
        owner.absorptionAmount = owner.absorptionAmount.coerceAtLeast(hearts)
    }

    override fun statValue(type: StatType): Double {
        val base = baseStats[type]
        val iv = record.ivs[type]
        val evContribution = record.evs[type] / 4.0
        val raw = base + iv + evContribution
        return currentTalents.fold(raw) { acc, talent -> talent.modifyStat(this, type, acc) }
    }

    fun toRecord(): PetRecord = currentRecord

    private fun tick() {
        val stand = armorStand ?: return
        if (!owner.isOnline || owner.isDead) {
            service.dismiss(owner, internal = true)
            return
        }

        val targetLocation = if (mounted) owner.location else owner.location.add(0.5, 1.0, 0.5)
        stand.teleport(targetLocation)
        currentTalents.forEach { it.tick(this) }

        if (attackCooldown > 0) attackCooldown--
        handleHealing()
        handleCombat(stand)
        if (flying) {
            owner.fallDistance = 0f
        }
    }

    private fun handleHealing() {
        val maxHealth = owner.maxHealth
        if (owner.health < maxHealth) {
            val healAmount = max(0.5, statValue(StatType.MAGIC) * 0.05)
            heal(healAmount)
        }
    }

    private fun handleCombat(stand: ArmorStand) {
        val world = stand.world
        val nearby = world.getNearbyEntities(stand.location, 8.0, 6.0, 8.0)
            .filterIsInstance<Monster>()
            .filter { it.isValid && !it.isDead }
        val target = nearby.minByOrNull { it.location.distanceSquared(stand.location) }
        if (target != null && attackCooldown <= 0) {
            val damage = statValue(StatType.ATTACK) * 0.75 + statValue(StatType.MAGIC) * 0.25
            dealDamage(target, damage)
            currentTalents.forEach { it.onAttack(this, target, damage) }
            attackCooldown = max(2, (6 - statValue(StatType.MAGIC) / 4).toInt())
        }
    }

    private fun dealDamage(target: LivingEntity, amount: Double) {
        target.damage(amount, owner)
        owner.sendActionBar(Component.text("§d${record.displayName} hits ${target.name} for ${"%.1f".format(amount)}"))
    }
}
