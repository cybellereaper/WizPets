package com.github.cybellereaper.wizPets.pet.model

import com.github.cybellereaper.wizPets.talent.TalentId
import kotlinx.serialization.Serializable

/**
 * Static definition for a species that can be captured or granted to players.
 */
@Serializable
data class PetSpecies(
    val id: String,
    val displayName: String,
    val element: Element,
    val statSheet: StatSheet,
    val baseLevel: Int,
    val defaultTalents: List<TalentId>,
)
