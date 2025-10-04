package com.github.cybellereaper.wizpets.core.talent.defaults

import com.github.cybellereaper.wizpets.api.ActivePet
import com.github.cybellereaper.wizpets.api.StatType
import com.github.cybellereaper.wizpets.api.talent.PetTalent
import org.bukkit.Particle

class HealingAuraTalent : PeriodicTalent(intervalTicks = 100), PetTalent {
    override val id: String = "healing_aura"
    override val displayName: String = "Healing Aura"
    override val description: String = "Every few seconds your pet mends your wounds."

    override fun trigger(pet: ActivePet) {
        val healAmount = 1.5 + pet.statValue(StatType.MAGIC) * 0.08
        pet.heal(healAmount)
        val location = pet.owner.location.add(0.0, 1.0, 0.0)
        pet.owner.world.spawnParticle(Particle.HEART, location, 5, 0.3, 0.4, 0.3, 0.0)
    }
}
