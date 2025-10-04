package com.github.cybellereaper.wizpets.api.talent

import com.github.cybellereaper.wizpets.api.ActivePet
import com.github.cybellereaper.wizpets.api.StatType
import org.bukkit.entity.LivingEntity

/**
 * Represents a behaviour module that can be attached to an active pet.
 */
interface PetTalent {
    val id: String
    val displayName: String
    val description: String

    fun onSummon(pet: ActivePet) {}
    fun tick(pet: ActivePet) {}
    fun onAttack(pet: ActivePet, target: LivingEntity, baseDamage: Double) {}
    fun onDismiss(pet: ActivePet) {}

    /**
     * Allows talents to influence stats before calculations are finalised.
     */
    fun modifyStat(pet: ActivePet, stat: StatType, current: Double): Double = current
}

fun interface TalentFactory {
    fun create(): PetTalent
}
