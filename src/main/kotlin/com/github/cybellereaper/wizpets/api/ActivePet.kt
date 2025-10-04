package com.github.cybellereaper.wizpets.api

import com.github.cybellereaper.wizpets.api.talent.PetTalent
import org.bukkit.entity.Player

/**
 * Read-only handle for an active pet instance with a small command surface for talents.
 */
interface ActivePet {
    val owner: Player
    val record: PetRecord
    val talents: List<PetTalent>
    val isMounted: Boolean
    val isFlying: Boolean

    fun statValue(type: StatType): Double
    fun statBreakdown(): Map<StatType, Double> = StatType.entries.associateWith { statValue(it) }

    fun heal(amount: Double)
    fun grantAbsorption(hearts: Double)
}
