package com.github.cybellereaper.wizPets.pet.service

import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import java.util.ArrayDeque

/**
 * Simple object pool for armour stands to reduce entity churn while moving pets around.
 */
class ArmorStandPool {
    private val recycled = ArrayDeque<ArmorStand>()

    fun borrow(location: Location): ArmorStand {
        val entity = findReusable() ?: spawn(location)
        reset(entity, location)
        return entity
    }

    fun recycle(stand: ArmorStand) {
        if (!stand.isValid) return
        stand.customName = null
        stand.isInvisible = true
        stand.isInvulnerable = true
        stand.removeScoreboardTag(TAG_ACTIVE)
        stand.removeScoreboardTag(TAG_PET)
        stand.equipment?.clear()
        recycled += stand
    }

    private fun findReusable(): ArmorStand? {
        while (recycled.isNotEmpty()) {
            val stand = recycled.removeFirst()
            if (stand.isValid && !stand.isDead) {
                return stand
            }
            stand.remove()
        }
        return null
    }

    private fun spawn(location: Location): ArmorStand {
        return location.world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
    }

    private fun reset(stand: ArmorStand, location: Location) {
        stand.teleport(location)
        stand.isInvisible = false
        stand.isInvulnerable = true
        stand.isMarker = true
        stand.setGravity(false)
        stand.customNameVisible = true
        stand.addScoreboardTag(TAG_ACTIVE)
        stand.addScoreboardTag(TAG_PET)
    }

    companion object {
        private const val TAG_ACTIVE = "wizpets_active"
        const val TAG_PET = "wizpets_pet"
    }
}

