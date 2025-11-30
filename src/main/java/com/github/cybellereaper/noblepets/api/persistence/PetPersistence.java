package com.github.cybellereaper.noblepets.api.persistence;

import com.github.cybellereaper.noblepets.api.PetRecord;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
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

  /** Determine whether a stored pet record is present on the given holder. */
  boolean exists(PersistentDataHolder holder);

  /** Convenience overload that targets a player directly. */
  default boolean exists(Player player) {
    return exists((PersistentDataHolder) player);
  }

  /**
   * Load an existing record or create and persist a new one via the supplied factory.
   *
   * <p>The freshly created record is automatically saved to the holder before being returned.
   */
  default PetRecord loadOrCreate(PersistentDataHolder holder, Supplier<PetRecord> factory) {
    return load(holder)
        .orElseGet(
            () -> {
              PetRecord record = factory.get();
              save(holder, record);
              return record;
            });
  }

  /** Convenience overload that targets a player directly. */
  default PetRecord loadOrCreate(Player player, Supplier<PetRecord> factory) {
    return loadOrCreate((PersistentDataHolder) player, factory);
  }

  /**
   * Atomically compute a new value for the stored record.
   *
   * <p>The provided operator receives the current record (if present) and should return the
   * replacement value. Returning an empty optional removes the stored record entirely, while a
   * present value is persisted before the optional is returned to the caller.
   */
  Optional<PetRecord> compute(
      PersistentDataHolder holder, Function<Optional<PetRecord>, Optional<PetRecord>> operation);

  /** Convenience overload that targets a player directly. */
  default Optional<PetRecord> compute(
      Player player, Function<Optional<PetRecord>, Optional<PetRecord>> operation) {
    return compute((PersistentDataHolder) player, operation);
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
