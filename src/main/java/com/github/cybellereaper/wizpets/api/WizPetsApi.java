package com.github.cybellereaper.wizpets.api;

import com.github.cybellereaper.wizpets.api.talent.TalentFactory;
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView;
import com.github.cybellereaper.wizpets.api.timeline.PetLifecycleListener;
import java.util.List;
import java.util.Optional;
import org.bukkit.entity.Player;

/** Entry point exposed to other plugins through Bukkit's services manager. */
public interface WizPetsApi {
  ActivePet activePet(Player player);

  PetRecord storedPet(Player player);

  default Optional<PetRecord> storedPetOptional(Player player) {
    return Optional.ofNullable(storedPet(player));
  }

  ActivePet summon(Player player, SummonReason reason);

  boolean dismiss(Player player, DismissReason reason);

  void persist(Player player);

  boolean mount(Player player);

  boolean dismount(Player player);

  boolean enableFlight(Player player);

  boolean disableFlight(Player player);

  void breed(Player player, Player partner);

  List<String> debugLines(Player player);

  TalentRegistryView talents();

  void registerTalent(TalentFactory factory, boolean replace);

  default void registerTalent(TalentFactory factory) {
    registerTalent(factory, false);
  }

  void unregisterTalent(String id);

  void addListener(PetLifecycleListener listener);

  void removeListener(PetLifecycleListener listener);
}
