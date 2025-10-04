package com.github.cybellereaper.wizpets.core.talent.defaults

import com.github.cybellereaper.wizpets.api.ActivePet
import com.github.cybellereaper.wizpets.api.StatType
import com.github.cybellereaper.wizpets.api.talent.PetTalent
import org.bukkit.Particle

class GuardianShellTalent : PeriodicTalent(intervalTicks = 240), PetTalent {
    override val id: String = "guardian_shell"
    override val displayName: String = "Guardian Shell"
    override val description: String = "Periodically grants absorption hearts to protect you."

    override fun trigger(pet: ActivePet) {
        val absorption = 2.0 + pet.statValue(StatType.DEFENSE) * 0.05
        pet.grantAbsorption(absorption)
        pet.owner.world.spawnParticle(Particle.INSTANT_EFFECT, pet.owner.location, 20, 0.5, 0.5, 0.5, 0.0)
    }
}
