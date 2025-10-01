package com.github.cybellereaper.wizPets.pet.model

import com.github.cybellereaper.wizPets.talent.TalentId
import java.util.UUID

data class PetSnapshot(
    val id: UUID,
    val nickname: String,
    val speciesId: String,
    val level: Int,
    val investments: StatInvestments,
    val talents: List<TalentId>,
)

