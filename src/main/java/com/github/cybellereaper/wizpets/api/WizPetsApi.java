package com.github.cybellereaper.wizpets.api;

import com.github.cybellereaper.wizpets.api.talent.TalentFactory;
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView;
import com.github.cybellereaper.wizpets.api.timeline.PetLifecycleListener;
import org.bukkit.entity.Player;

/**
 * Entry point exposed to other plugins through Bukkit's services manager.
 */
public interface WizPetsApi {
    ActivePet activePet(Player player);

    PetRecord storedPet(Player player);

    ActivePet summon(Player player, SummonReason reason);

    boolean dismiss(Player player, DismissReason reason);

    void persist(Player player);

    TalentRegistryView talents();

    void registerTalent(TalentFactory factory, boolean replace);

    default void registerTalent(TalentFactory factory) {
        registerTalent(factory, false);
    }

    void unregisterTalent(String id);

    void addListener(PetLifecycleListener listener);

    void removeListener(PetLifecycleListener listener);
}
