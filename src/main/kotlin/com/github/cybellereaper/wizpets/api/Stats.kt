package com.github.cybellereaper.wizpets.api

import kotlin.math.max
import kotlin.random.Random

/**
 * Immutable set of battle stats used by pets.
 */
data class StatSet(
    val health: Double,
    val attack: Double,
    val defense: Double,
    val magic: Double
) {
    operator fun get(type: StatType): Double = when (type) {
        StatType.HEALTH -> health
        StatType.ATTACK -> attack
        StatType.DEFENSE -> defense
        StatType.MAGIC -> magic
    }

    fun update(type: StatType, value: Double): StatSet = when (type) {
        StatType.HEALTH -> copy(health = value)
        StatType.ATTACK -> copy(attack = value)
        StatType.DEFENSE -> copy(defense = value)
        StatType.MAGIC -> copy(magic = value)
    }

    fun breedWith(other: StatSet, random: Random): StatSet {
        fun mix(a: Double, b: Double): Double {
            val base = (a + b) / 2.0
            val variation = random.nextDouble(-2.75, 2.75)
            return max(1.0, base + variation)
        }
        return StatSet(
            health = mix(health, other.health),
            attack = mix(attack, other.attack),
            defense = mix(defense, other.defense),
            magic = mix(magic, other.magic)
        )
    }

    companion object {
        fun randomEV(random: Random): StatSet = StatSet(
            health = random.nextDouble(0.0, 64.0),
            attack = random.nextDouble(0.0, 64.0),
            defense = random.nextDouble(0.0, 64.0),
            magic = random.nextDouble(0.0, 64.0)
        )

        fun randomIV(random: Random): StatSet = StatSet(
            health = random.nextDouble(0.0, 15.0),
            attack = random.nextDouble(0.0, 15.0),
            defense = random.nextDouble(0.0, 15.0),
            magic = random.nextDouble(0.0, 15.0)
        )
    }
}

enum class StatType(val displayName: String) {
    HEALTH("Health"),
    ATTACK("Attack"),
    DEFENSE("Defense"),
    MAGIC("Magic")
}
