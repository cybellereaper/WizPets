package com.github.cybellereaper.wizpets.core.talent.defaults

import com.github.cybellereaper.wizpets.api.ActivePet
import com.github.cybellereaper.wizpets.api.StatType
import com.github.cybellereaper.wizpets.api.talent.PetTalent
import net.kyori.adventure.text.Component
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity

class ArcaneBurstTalent : PetTalent {
    override val id: String = "arcane_burst"
    override val displayName: String = "Arcane Burst"
    override val description: String = "Your pet's strikes occasionally unleash extra arcane damage."

    private var cooldown = 0

    override fun tick(pet: ActivePet) {
        if (cooldown > 0) cooldown--
    }

    override fun onAttack(pet: ActivePet, target: LivingEntity, baseDamage: Double) {
        if (cooldown > 0) return
        val bonus = pet.statValue(StatType.MAGIC) * 0.6
        target.damage(bonus, pet.owner)
        target.world.spawnParticle(Particle.END_ROD, target.location.add(0.0, 1.0, 0.0), 15, 0.2, 0.4, 0.2, 0.0)
        pet.owner.sendActionBar(Component.text("§5Arcane Burst deals ${"%.1f".format(bonus)} bonus damage!"))
        cooldown = 60
    }
}
