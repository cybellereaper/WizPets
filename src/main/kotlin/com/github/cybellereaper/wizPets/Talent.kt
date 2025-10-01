package com.github.cybellereaper.wizPets

import net.kyori.adventure.text.Component
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import kotlin.random.Random

interface Talent {
    val id: String
    val displayName: String
    val description: String

    fun onSummon(pet: Pet) {}
    fun tick(pet: Pet) {}
    fun onAttack(pet: Pet, target: LivingEntity, baseDamage: Double) {}
    fun onDismiss(pet: Pet) {}
}

abstract class PeriodicTalent(private val intervalSeconds: Int) : Talent {
    private var ticks = 0
    final override fun tick(pet: Pet) {
        ticks++
        if (ticks >= intervalSeconds) {
            ticks = 0
            trigger(pet)
        }
    }

    protected abstract fun trigger(pet: Pet)
}

class HealingAuraTalent : PeriodicTalent(intervalSeconds = 5) {
    override val id: String = "healing_aura"
    override val displayName: String = "Healing Aura"
    override val description: String = "Every few seconds your pet mends your wounds."

    override fun trigger(pet: Pet) {
        val healAmount = 1.5 + pet.getStat(StatType.MAGIC) * 0.08
        pet.heal(healAmount)
        pet.owner.world.spawnParticle(Particle.HEART, pet.owner.location.add(0.0, 1.0, 0.0), 5, 0.3, 0.4, 0.3, 0.0)
    }
}

class GuardianShellTalent : PeriodicTalent(intervalSeconds = 12) {
    override val id: String = "guardian_shell"
    override val displayName: String = "Guardian Shell"
    override val description: String = "Periodically grants absorption hearts to protect you."

    override fun trigger(pet: Pet) {
        val absorption = 2.0 + pet.getStat(StatType.DEFENSE) * 0.05
        pet.grantAbsorption(absorption)
        pet.owner.world.spawnParticle(Particle.SPELL_INSTANT, pet.owner.location, 20, 0.5, 0.5, 0.5, 0.0)
    }
}

class ArcaneBurstTalent : Talent {
    override val id: String = "arcane_burst"
    override val displayName: String = "Arcane Burst"
    override val description: String = "Your pet's strikes occasionally unleash extra arcane damage."
    private var cooldown = 0

    override fun tick(pet: Pet) {
        if (cooldown > 0) cooldown--
    }

    override fun onAttack(pet: Pet, target: LivingEntity, baseDamage: Double) {
        if (cooldown <= 0) {
            val bonus = pet.getStat(StatType.MAGIC) * 0.6
            target.damage(bonus, pet.owner)
            target.world.spawnParticle(Particle.END_ROD, target.location.add(0.0, 1.0, 0.0), 15, 0.2, 0.4, 0.2, 0.0)
            pet.owner.sendActionBar(Component.text("§5Arcane Burst deals ${"%.1f".format(bonus)} bonus damage!"))
            cooldown = 6
        }
    }
}

object TalentRegistry {
    private val availableTalents = listOf<(Random) -> Talent>(
        { HealingAuraTalent() },
        { GuardianShellTalent() },
        { ArcaneBurstTalent() }
    )

    fun rollTalents(random: Random, count: Int = 2): List<Talent> {
        return buildList {
            repeat(count) {
                val factory = availableTalents[random.nextInt(availableTalents.size)]
                add(factory(random))
            }
        }
    }
}
