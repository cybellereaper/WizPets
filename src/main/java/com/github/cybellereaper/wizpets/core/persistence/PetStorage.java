package com.github.cybellereaper.wizpets.core.persistence;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.api.persistence.PetPersistence;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Option;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jooq.lambda.Seq;

public final class PetStorage implements PetPersistence {
  private final NamespacedKey rootKey;
  private final NamespacedKey nameKey;
  private final NamespacedKey generationKey;
  private final NamespacedKey breedKey;
  private final NamespacedKey talentsKey;
  private final NamespacedKey mountKey;
  private final NamespacedKey flightKey;
  private final NamespacedKey evHealthKey;
  private final NamespacedKey evAttackKey;
  private final NamespacedKey evDefenseKey;
  private final NamespacedKey evMagicKey;
  private final NamespacedKey ivHealthKey;
  private final NamespacedKey ivAttackKey;
  private final NamespacedKey ivDefenseKey;
  private final NamespacedKey ivMagicKey;

  public PetStorage(JavaPlugin plugin) {
    JavaPlugin safePlugin = Objects.requireNonNull(plugin, "plugin");
    rootKey = new NamespacedKey(safePlugin, "pet");
    nameKey = new NamespacedKey(safePlugin, "name");
    generationKey = new NamespacedKey(safePlugin, "generation");
    breedKey = new NamespacedKey(safePlugin, "breed_count");
    talentsKey = new NamespacedKey(safePlugin, "talents");
    mountKey = new NamespacedKey(safePlugin, "mount_unlocked");
    flightKey = new NamespacedKey(safePlugin, "flight_unlocked");
    evHealthKey = new NamespacedKey(safePlugin, "ev_health");
    evAttackKey = new NamespacedKey(safePlugin, "ev_attack");
    evDefenseKey = new NamespacedKey(safePlugin, "ev_defense");
    evMagicKey = new NamespacedKey(safePlugin, "ev_magic");
    ivHealthKey = new NamespacedKey(safePlugin, "iv_health");
    ivAttackKey = new NamespacedKey(safePlugin, "iv_attack");
    ivDefenseKey = new NamespacedKey(safePlugin, "iv_defense");
    ivMagicKey = new NamespacedKey(safePlugin, "iv_magic");
  }

  @Override
  public Optional<PetRecord> load(PersistentDataHolder holder) {
    PersistentDataHolder safeHolder = Objects.requireNonNull(holder, "holder");
    return decodeFromParent(safeHolder.getPersistentDataContainer());
  }

  @Override
  public void save(PersistentDataHolder holder, PetRecord record) {
    PersistentDataHolder safeHolder = Objects.requireNonNull(holder, "holder");
    PetRecord safeRecord = Objects.requireNonNull(record, "record");
    PersistentDataContainer parent = safeHolder.getPersistentDataContainer();
    PersistentDataContainer container = encode(parent.getAdapterContext(), safeRecord);
    parent.set(rootKey, PersistentDataType.TAG_CONTAINER, container);
  }

  @Override
  public void clear(PersistentDataHolder holder) {
    Objects.requireNonNull(holder, "holder").getPersistentDataContainer().remove(rootKey);
  }

  @Override
  public boolean exists(PersistentDataHolder holder) {
    return Objects.requireNonNull(holder, "holder")
        .getPersistentDataContainer()
        .has(rootKey, PersistentDataType.TAG_CONTAINER);
  }

  @Override
  public Optional<PetRecord> compute(
      PersistentDataHolder holder,
      Function<Optional<PetRecord>, Optional<PetRecord>> operation) {
    PersistentDataHolder safeHolder = Objects.requireNonNull(holder, "holder");
    Function<Optional<PetRecord>, Optional<PetRecord>> safeOperation =
        Objects.requireNonNull(operation, "operation");
    PersistentDataContainer parent = safeHolder.getPersistentDataContainer();
    Optional<PetRecord> current = decodeFromParent(parent);
    Optional<PetRecord> result = Objects.requireNonNull(safeOperation.apply(current));
    if (result.isPresent()) {
      save(safeHolder, result.get());
    } else {
      clear(safeHolder);
    }
    return result;
  }

  @Override
  public PersistentDataContainer encode(
      PersistentDataAdapterContext context, PetRecord record) {
    PersistentDataAdapterContext safeContext = Objects.requireNonNull(context, "context");
    PetRecord safeRecord = Objects.requireNonNull(record, "record");
    PersistentDataContainer container = safeContext.newPersistentDataContainer();

    container.set(nameKey, PersistentDataType.STRING, safeRecord.displayName());
    container.set(generationKey, PersistentDataType.INTEGER, safeRecord.generation());
    container.set(breedKey, PersistentDataType.INTEGER, safeRecord.breedCount());
    container.set(talentsKey, PersistentDataType.STRING, String.join(";", safeRecord.talentIds()));
    container.set(mountKey, PersistentDataType.BYTE, asByte(safeRecord.mountUnlocked()));
    container.set(flightKey, PersistentDataType.BYTE, asByte(safeRecord.flightUnlocked()));

    container.set(evHealthKey, PersistentDataType.DOUBLE, safeRecord.evs().health());
    container.set(evAttackKey, PersistentDataType.DOUBLE, safeRecord.evs().attack());
    container.set(evDefenseKey, PersistentDataType.DOUBLE, safeRecord.evs().defense());
    container.set(evMagicKey, PersistentDataType.DOUBLE, safeRecord.evs().magic());

    container.set(ivHealthKey, PersistentDataType.DOUBLE, safeRecord.ivs().health());
    container.set(ivAttackKey, PersistentDataType.DOUBLE, safeRecord.ivs().attack());
    container.set(ivDefenseKey, PersistentDataType.DOUBLE, safeRecord.ivs().defense());
    container.set(ivMagicKey, PersistentDataType.DOUBLE, safeRecord.ivs().magic());

    return container;
  }

  @Override
  public Optional<PetRecord> decode(PersistentDataContainer container) {
    PersistentDataContainer safeContainer = Objects.requireNonNull(container, "container");
    return Option.of(safeContainer.get(nameKey, PersistentDataType.STRING))
        .filter(name -> !name.isBlank())
        .map(name -> buildRecord(safeContainer, name))
        .toJavaOptional();
  }

  private Optional<PetRecord> decodeFromParent(PersistentDataContainer parent) {
    PersistentDataContainer safeParent = Objects.requireNonNull(parent, "parent");
    return Option.of(safeParent.get(rootKey, PersistentDataType.TAG_CONTAINER))
        .flatMap(container -> Option.ofOptional(decode(container)))
        .toJavaOptional();
  }

  private PetRecord buildRecord(PersistentDataContainer container, String name) {
    PersistentDataContainer safeContainer = Objects.requireNonNull(container, "container");
    Objects.requireNonNull(name, "name");
    List<String> talents =
        Option.of(safeContainer.get(talentsKey, PersistentDataType.STRING))
            .filter(raw -> !raw.isBlank())
            .map(
                raw ->
                    Seq.of(raw.split(";"))
                        .map(String::trim)
                        .filter(trimmed -> !trimmed.isEmpty())
                        .toList())
            .map(ImmutableList::copyOf)
            .getOrElse(ImmutableList::of);

    StatSet evs =
        new StatSet(
            getDouble(safeContainer, evHealthKey),
            getDouble(safeContainer, evAttackKey),
            getDouble(safeContainer, evDefenseKey),
            getDouble(safeContainer, evMagicKey));

    StatSet ivs =
        new StatSet(
            getDouble(safeContainer, ivHealthKey),
            getDouble(safeContainer, ivAttackKey),
            getDouble(safeContainer, ivDefenseKey),
            getDouble(safeContainer, ivMagicKey));

    int generation =
        Option.of(safeContainer.get(generationKey, PersistentDataType.INTEGER)).getOrElse(1);
    int breedCount =
        Option.of(safeContainer.get(breedKey, PersistentDataType.INTEGER)).getOrElse(0);
    boolean mountUnlocked =
        Option.of(safeContainer.get(mountKey, PersistentDataType.BYTE))
            .exists(value -> value == (byte) 1);
    boolean flightUnlocked =
        Option.of(safeContainer.get(flightKey, PersistentDataType.BYTE))
            .exists(value -> value == (byte) 1);

    return new PetRecord(
        name, evs, ivs, talents, generation, breedCount, mountUnlocked, flightUnlocked);
  }

  private static double getDouble(PersistentDataContainer container, NamespacedKey key) {
    Double value = container.get(key, PersistentDataType.DOUBLE);
    return value != null ? value : 0.0;
  }

  private static byte asByte(boolean value) {
    return (byte) (value ? 1 : 0);
  }
}
