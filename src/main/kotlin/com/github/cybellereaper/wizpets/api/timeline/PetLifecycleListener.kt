package com.github.cybellereaper.wizpets.api.timeline

import com.github.cybellereaper.wizpets.api.ActivePet
import com.github.cybellereaper.wizpets.api.DismissReason
import com.github.cybellereaper.wizpets.api.PetRecord
import com.github.cybellereaper.wizpets.api.SummonReason
import org.bukkit.entity.Player

/**
 * Callback interface that external plugins can implement to observe lifecycle events.
 */
interface PetLifecycleListener {
    fun onSummoned(player: Player, pet: ActivePet, reason: SummonReason) {}
    fun onDismissed(player: Player, record: PetRecord?, reason: DismissReason) {}
    fun onPersist(player: Player, record: PetRecord) {}
}
