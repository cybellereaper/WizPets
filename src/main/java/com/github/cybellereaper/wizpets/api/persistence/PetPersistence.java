package com.github.cybellereaper.wizpets.api.persistence;

import com.github.cybellereaper.wizpets.api.PetRecord;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;

/** High-level access to persisted pet records backed by Bukkit's data containers. */
public interface PetPersistence {
  /** Load a stored pet record from the supplied holder. */
  Optional<PetRecord> load(PersistentDataHolder holder);

  /** Convenience overload that targets a player directly. */
  default Optional<PetRecord> load(Player player) {
    return load((PersistentDataHolder) player);
  }

  /** Persist a record into the holder's underlying data container. */
  void save(PersistentDataHolder holder, PetRecord record);

  /** Convenience overload that targets a player directly. */
  default void save(Player player, PetRecord record) {
    save((PersistentDataHolder) player, record);
  }

  /** Remove any stored record attached to the holder. */
  void clear(PersistentDataHolder holder);

  /** Convenience overload that targets a player directly. */
  default void clear(Player player) {
    clear((PersistentDataHolder) player);
  }

  /**
   * Decode a raw persistent container into a pet record.
   *
   * <p>The container should represent the serialized form produced by {@link #encode}.
   */
  Optional<PetRecord> decode(PersistentDataContainer container);

  /** Serialize a pet record into a new persistent container using the provided context. */
  PersistentDataContainer encode(PersistentDataAdapterContext context, PetRecord record);
}
