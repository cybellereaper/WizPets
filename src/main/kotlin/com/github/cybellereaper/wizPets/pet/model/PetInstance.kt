package com.github.cybellereaper.wizPets.pet.model

import com.github.cybellereaper.wizPets.pet.service.ArmorStandPool
import com.github.cybellereaper.wizPets.talent.TalentContext
import com.github.cybellereaper.wizPets.talent.TalentId
import com.github.cybellereaper.wizPets.talent.TalentRegistry
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * Runtime state for a player-owned pet.
 */
class PetInstance(
    val id: UUID,
    val ownerId: UUID,
    val nickname: String,
    val species: PetSpecies,
    var level: Int,
    val investments: StatInvestments,
    private val talents: List<TalentId>,
) {
    private var currentHealth: Int = stats().maxHealth
    private var stand: ArmorStand? = null
    private val inventory: MutableList<ItemStack> = mutableListOf()

    fun summon(owner: Player, pool: ArmorStandPool, location: Location) {
        if (stand?.isValid == true) return
        val entity = pool.borrow(location)
        entity.customName = nickname
        entity.isCustomNameVisible = true
        stand = entity
    }

    fun dismiss(pool: ArmorStandPool) {
        stand?.let(pool::recycle)
        stand = null
    }

    fun tick(owner: Player, registry: TalentRegistry) {
        val armorStand = stand ?: return
        val stats = stats()
        if (currentHealth < stats.maxHealth) {
            currentHealth = (currentHealth + stats.support / 10).coerceAtMost(stats.maxHealth)
        }
        follow(owner, armorStand)
        runTalents(owner, armorStand, registry)
    }

    fun damage(amount: Double): Boolean {
        currentHealth -= amount.toInt()
        return currentHealth <= 0
    }

    fun snapshot(): PetSnapshot = PetSnapshot(
        id = id,
        nickname = nickname,
        speciesId = species.id,
        level = level,
        investments = investments,
        talents = talents,
    )

    private fun stats() = investments.toSheet(species.statSheet).atLevel(level)

    private fun follow(owner: Player, armorStand: ArmorStand) {
        val ownerLocation = owner.location
        if (armorStand.location.distanceSquared(ownerLocation) > 9) {
            armorStand.teleport(ownerLocation.clone().add(0.5, 0.0, 0.5))
        }
    }

    private fun runTalents(owner: Player, armorStand: ArmorStand, registry: TalentRegistry) {
        val context = TalentContext(armorStand, owner, level, inventory)
        talents.forEach { registry.get(it).tick(context) }
    }
}

