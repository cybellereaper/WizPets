package com.github.cybellereaper.wizPets.talent

import org.bukkit.Particle
import kotlin.math.min

internal object MedicTalent {
    fun perform(context: TalentContext) {
        val owner = context.owner
        if (owner.health >= owner.maxHealth) return

        val healAmount = min(4.0, 1.0 + context.level * 0.35)
        owner.health = min(owner.maxHealth, owner.health + healAmount)
        owner.world.spawnParticle(Particle.HEART, owner.location.add(0.0, 1.0, 0.0), 4, 0.3, 0.5, 0.3, 0.0)
    }
}

