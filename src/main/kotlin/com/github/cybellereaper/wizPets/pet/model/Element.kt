package com.github.cybellereaper.wizPets.pet.model

import kotlinx.serialization.Serializable

/**
 * Simplified elemental affinity list used for damage adjustments and task bonuses.
 */
@Serializable
enum class Element {
    FLAME,
    FROST,
    STORM,
    TERRA,
    AQUA,
    ARCANE,
    NATURE,
}
