package com.github.cybellereaper.wizpets.api.talent

/**
 * Read-only view of the registered talent factories.
 */
interface TalentRegistryView : Iterable<PetTalentDescriptor> {
    operator fun get(id: String): PetTalentDescriptor?
    val size: Int
}

data class PetTalentDescriptor(
    val id: String,
    val displayName: String,
    val description: String
)
