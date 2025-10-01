package com.github.cybellereaper.wizPets

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.data.Ageable
import org.bukkit.entity.LivingEntity
import kotlin.math.min
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

class HealingAuraTalent : PeriodicTalent(intervalSeconds = 6) {
    override val id: String = "healing_aura"
    override val displayName: String = "Healing Aura"
    override val description: String = "Every few seconds your pet mends your wounds."

    override fun trigger(pet: Pet) {
        val healAmount = 2.0 + pet.getStat(StatType.MAGIC) * 0.08
        pet.heal(healAmount)
        pet.owner.world.spawnParticle(Particle.HEART, pet.owner.location.add(0.0, 1.0, 0.0), 6, 0.3, 0.4, 0.3, 0.0)
    }
}

class GuardianShellTalent : PeriodicTalent(intervalSeconds = 12) {
    override val id: String = "guardian_shell"
    override val displayName: String = "Guardian Shell"
    override val description: String = "Periodically grants absorption hearts to protect you."

    override fun trigger(pet: Pet) {
        val absorption = 2.0 + pet.getStat(StatType.DEFENSE) * 0.06
        pet.grantAbsorption(absorption)
        pet.owner.world.spawnParticle(Particle.SPELL_INSTANT, pet.owner.location, 18, 0.6, 0.6, 0.6, 0.0)
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
            val bonus = pet.getStat(StatType.MAGIC) * 0.65
            target.damage(bonus, pet.owner)
            target.world.spawnParticle(Particle.END_ROD, target.location.add(0.0, 1.0, 0.0), 18, 0.2, 0.4, 0.2, 0.0)
            pet.owner.sendActionBar(Component.text("§5Arcane Burst deals ${"%.1f".format(bonus)} bonus damage!"))
            cooldown = 8
        }
    }
}

class VerdantCaretakerTalent : PeriodicTalent(intervalSeconds = 10) {
    override val id: String = "verdant_caretaker"
    override val displayName: String = "Verdant Caretaker"
    override val description: String = "Encourages nearby crops and saplings to grow."

    override fun trigger(pet: Pet) {
        val world = pet.owner.world
        val center = pet.owner.location.block.location
        var boosted = false
        for (x in -3..3) {
            for (z in -3..3) {
                val block = center.clone().add(x.toDouble(), -1.0, z.toDouble()).block
                val above = block.location.add(0.0, 1.0, 0.0).block
                val data = above.blockData
                if (data is Ageable && data.age < data.maximumAge) {
                    data.age = min(data.age + 1, data.maximumAge)
                    above.blockData = data
                    world.spawnParticle(Particle.VILLAGER_HAPPY, above.location.add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, 0.0)
                    boosted = true
                    break
                }
            }
            if (boosted) break
        }
        if (boosted) {
            pet.owner.sendActionBar(Component.text("§a${pet.displayName} tends to your garden!"))
        }
    }
}

class EssenceHarvesterTalent : Talent {
    override val id: String = "essence_harvester"
    override val displayName: String = "Essence Harvester"
    override val description: String = "Increases item collection radius and grants a chance for bonus drops."

    override fun onSummon(pet: Pet) {
        pet.extraCollectionRange += 1.5
    }

    override fun onDismiss(pet: Pet) {
        pet.extraCollectionRange -= 1.5
    }

    override fun onAttack(pet: Pet, target: LivingEntity, baseDamage: Double) {
        if (target is org.bukkit.entity.Player) return
        if (!target.isDead && target.health > 0.0) return
        if (Random.nextDouble() < 0.25) {
            target.world.dropItemNaturally(target.location, Material.EXPERIENCE_BOTTLE.createItemStack())
        }
    }
}

class ElementalWardTalent : Talent {
    override val id: String = "elemental_ward"
    override val displayName: String = "Elemental Ward"
    override val description: String = "Grants periodic elemental resistance based on your pet's typing."
    private var timer = 0

    override fun tick(pet: Pet) {
        timer++
        if (timer >= 15) {
            timer = 0
            val resistance = 4 + (pet.getStat(StatType.DEFENSE) / 4.0).toInt()
            val effect = pet.element.createPotionEffect(resistance)
            effect?.let { pet.owner.addPotionEffect(it) }
            pet.owner.world.spawnParticle(Particle.SPELL_WITCH, pet.owner.location.add(0.0, 1.0, 0.0), 20, 0.4, 0.5, 0.4, 0.0)
        }
    }
}

private fun Material.createItemStack() = org.bukkit.inventory.ItemStack(this)

private fun Element.createPotionEffect(resistance: Int) = when (this) {
    Element.FIRE -> org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 20 * 20, 0, false, false, true)
    Element.WATER -> org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING, 20 * 20, 0, false, false, true)
    Element.STORM -> org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE, 20 * 15, 0, false, false, true)
    Element.NATURE -> org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 20 * 10, 0, false, false, true)
    Element.ICE -> org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE, 20 * 15, 0, false, false, true)
    Element.MYTH -> org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 20 * 15, resistance.coerceAtLeast(1) - 1, false, false, true)
    Element.ARCANE -> org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE, 20 * 15, 0, false, false, true)
}

object TalentRegistry {
    private val factories: Map<String, (Random) -> Talent> = mapOf(
        "healing_aura" to { HealingAuraTalent() },
        "guardian_shell" to { GuardianShellTalent() },
        "arcane_burst" to { ArcaneBurstTalent() },
        "verdant_caretaker" to { VerdantCaretakerTalent() },
        "essence_harvester" to { EssenceHarvesterTalent() },
        "elemental_ward" to { ElementalWardTalent() }
    )

    fun rollTalents(random: Random, species: PetSpecies, count: Int = 2): List<String> {
        val pool = species.talentPool.map { it.lowercase() }.ifEmpty { factories.keys.toList() }
        if (pool.isEmpty()) return emptyList()
        val selections = mutableSetOf<String>()
        while (selections.size < min(count, pool.size)) {
            selections += pool[random.nextInt(pool.size)]
        }
        return selections.toList()
    }

    fun instantiate(ids: Collection<String>, random: Random): List<Talent> = ids.mapNotNull { createTalent(it, random) }

    fun createTalent(id: String, random: Random = Random.Default): Talent? = factories[id.lowercase()]?.invoke(random)
}
