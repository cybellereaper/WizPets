package com.github.cybellereaper.wizpets.api

import com.github.cybellereaper.wizpets.api.talent.TalentFactory
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView
import com.github.cybellereaper.wizpets.api.timeline.PetLifecycleListener
import org.bukkit.entity.Player

/**
 * Entry point exposed to other plugins through Bukkit's services manager.
 */
interface WizPetsApi {
    /** Returns the active pet handle for the given player if one is summoned. */
    fun activePet(player: Player): ActivePet?

    /** Returns the stored pet record for the given player or null if none exists. */
    fun storedPet(player: Player): PetRecord?

    /** Immediately summons or respawns the player's pet. */
    fun summon(player: Player, reason: SummonReason = SummonReason.MANUAL): ActivePet

    /** Dismisses the player's active pet if present. */
    fun dismiss(player: Player, reason: DismissReason = DismissReason.MANUAL): Boolean

    /** Persists the current state of the pet without despawning it. */
    fun persist(player: Player)

    /** Provides read-only access to the currently registered talents. */
    fun talents(): TalentRegistryView

    /** Registers a new talent factory at runtime. */
    fun registerTalent(factory: TalentFactory, replace: Boolean = false)

    /** Unregisters a talent. Active pets are refreshed to remove it. */
    fun unregisterTalent(id: String)

    /** Adds a lifecycle listener that will be notified for key events. */
    fun addListener(listener: PetLifecycleListener)

    /** Removes a previously registered lifecycle listener. */
    fun removeListener(listener: PetLifecycleListener)
}

enum class SummonReason {
    MANUAL,
    AUTO_SUMMON,
    BREEDING_REFRESH,
    RESTORE
}

enum class DismissReason {
    MANUAL,
    PLAYER_QUIT,
    PLUGIN_DISABLE
}
