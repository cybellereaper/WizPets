package com.github.cybellereaper.wizPets.pet.model

import com.github.cybellereaper.wizPets.serialization.UUIDSerializer
import com.github.cybellereaper.wizPets.talent.TalentId
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PetSnapshot(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val nickname: String,
    val speciesId: String,
    val level: Int,
    val investments: StatInvestments,
    val talents: List<TalentId>,
)
