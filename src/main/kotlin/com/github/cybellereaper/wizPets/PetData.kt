package com.github.cybellereaper.wizPets

data class PetData(
    val displayName: String,
    val evs: StatSet,
    val ivs: StatSet,
    val talentIds: List<String>,
    val generation: Int,
    val breedCount: Int,
    val mountUnlocked: Boolean,
    val flightUnlocked: Boolean,
    val behaviorScript: String
)
