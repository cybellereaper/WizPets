package com.github.cybellereaper.wizpets.api

/**
 * Immutable snapshot of a pet stored in persistent data.
 */
data class PetRecord(
    val displayName: String,
    val evs: StatSet,
    val ivs: StatSet,
    val talentIds: List<String>,
    val generation: Int,
    val breedCount: Int,
    val mountUnlocked: Boolean,
    val flightUnlocked: Boolean
) {
    fun withDisplayName(name: String) = copy(displayName = name)
}
