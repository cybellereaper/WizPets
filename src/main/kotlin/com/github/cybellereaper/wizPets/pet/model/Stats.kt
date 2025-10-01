package com.github.cybellereaper.wizPets.pet.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Represents the effort value (EV), individual value (IV), and base stat for a particular attribute.
 */
@Serializable
data class StatLine(
    val base: Int = 1,
    val effort: Int = 0,
    val individual: Int = 0,
) {
    init {
        require(base >= 1) { "Base stat must be positive" }
        require(effort >= 0) { "Effort value cannot be negative" }
        require(individual in 0..31) { "Individual value must be within the standard IV range" }
    }

    fun total(level: Int): Int {
        val levelMultiplier = max(level, 1)
        val evComponent = (effort / 4.0).roundToInt()
        return ((2 * base + individual + evComponent) * levelMultiplier) / 100 + 5
    }
}

/**
 * Effort and individual values without the base stat component.
 */
@Serializable
data class StatInvestment(
    val effort: Int = 0,
    val individual: Int = 0,
) {
    fun toLine(base: Int): StatLine = StatLine(base, effort, individual)
}

@Serializable
data class StatInvestments(
    val stamina: StatInvestment = StatInvestment(),
    val power: StatInvestment = StatInvestment(),
    val defense: StatInvestment = StatInvestment(),
    val focus: StatInvestment = StatInvestment(),
) {
    fun toSheet(baseSheet: StatSheet): StatSheet = StatSheet(
        stamina = stamina.toLine(baseSheet.stamina.base),
        power = power.toLine(baseSheet.power.base),
        defense = defense.toLine(baseSheet.defense.base),
        focus = focus.toLine(baseSheet.focus.base),
    )
}

/**
 * Simple container for a pet's combat-relevant stat lines.
 */
@Serializable
data class StatSheet(
    val stamina: StatLine = StatLine(),
    val power: StatLine = StatLine(),
    val defense: StatLine = StatLine(),
    val focus: StatLine = StatLine(),
) {
    fun atLevel(level: Int): CombatStats = CombatStats(
        maxHealth = stamina.total(level) * 5,
        attack = power.total(level),
        protection = defense.total(level),
        support = focus.total(level),
    )
}

/**
 * Runtime combat values derived from the stat sheet.
 */
@Serializable
data class CombatStats(
    val maxHealth: Int,
    val attack: Int,
    val protection: Int,
    val support: Int,
)
