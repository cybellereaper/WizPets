package com.github.cybellereaper.wizpets.api.timeline;

import com.github.cybellereaper.wizpets.api.ActivePet;
import com.github.cybellereaper.wizpets.api.DismissReason;
import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.SummonReason;
import org.bukkit.entity.Player;

/** Callback interface that external plugins can implement to observe lifecycle events. */
public interface PetLifecycleListener {
  default void onSummoned(Player player, ActivePet pet, SummonReason reason) {}

  default void onDismissed(Player player, PetRecord record, DismissReason reason) {}

  default void onPersist(Player player, PetRecord record) {}
}
