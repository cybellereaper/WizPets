package com.github.cybellereaper.wizPets.talent

import org.bukkit.entity.Monster
import org.bukkit.util.Vector
import kotlin.math.max

internal object SentinelTalent {
    fun perform(context: TalentContext) {
        val origin = context.armorStand.location
        val world = origin.world
        val range = 10.0
        val target = world.getNearbyEntities(origin, range, range, range)
            .filterIsInstance<Monster>()
            .filter { it.isValid && !it.isDead }
            .minByOrNull { it.location.distanceSquared(origin) }
            ?: return

        val damage = max(2.0, context.level * 0.6 + context.level % 5)
        target.damage(damage, context.owner)

        val knockback = Vector(origin.x - target.location.x, 0.3, origin.z - target.location.z)
        target.velocity = knockback.normalize().multiply(0.6)
    }
}

