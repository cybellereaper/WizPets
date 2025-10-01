package com.github.cybellereaper.wizPets

import kotlin.random.Random

data class StatSet(
    val health: Double,
    val attack: Double,
    val defense: Double,
    val magic: Double,
    val speed: Double
) {
    operator fun get(type: StatType): Double = when (type) {
        StatType.HEALTH -> health
        StatType.ATTACK -> attack
        StatType.DEFENSE -> defense
        StatType.MAGIC -> magic
        StatType.SPEED -> speed
    }

    companion object {
        fun randomEV(random: Random): StatSet = StatSet(
            health = random.nextDouble(0.0, 252.0),
            attack = random.nextDouble(0.0, 252.0),
            defense = random.nextDouble(0.0, 252.0),
            magic = random.nextDouble(0.0, 252.0),
            speed = random.nextDouble(0.0, 252.0)
        )

        fun randomIV(random: Random): StatSet = StatSet(
            health = random.nextDouble(0.0, 31.0),
            attack = random.nextDouble(0.0, 31.0),
            defense = random.nextDouble(0.0, 31.0),
            magic = random.nextDouble(0.0, 31.0),
            speed = random.nextDouble(0.0, 31.0)
        )
    }
}

enum class StatType(val displayName: String) {
    HEALTH("Health"),
    ATTACK("Attack"),
    DEFENSE("Defense"),
    MAGIC("Magic"),
    SPEED("Speed")
}
