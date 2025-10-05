package com.github.cybellereaper.wizpets.core.persistence;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.api.persistence.PetPersistence;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Option;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.NonNull;
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

  public PetStorage(@NonNull JavaPlugin plugin) {
    rootKey = new NamespacedKey(plugin, "pet");
    nameKey = new NamespacedKey(plugin, "name");
    generationKey = new NamespacedKey(plugin, "generation");
    breedKey = new NamespacedKey(plugin, "breed_count");
    talentsKey = new NamespacedKey(plugin, "talents");
    mountKey = new NamespacedKey(plugin, "mount_unlocked");
    flightKey = new NamespacedKey(plugin, "flight_unlocked");
    evHealthKey = new NamespacedKey(plugin, "ev_health");
    evAttackKey = new NamespacedKey(plugin, "ev_attack");
    evDefenseKey = new NamespacedKey(plugin, "ev_defense");
    evMagicKey = new NamespacedKey(plugin, "ev_magic");
    ivHealthKey = new NamespacedKey(plugin, "iv_health");
    ivAttackKey = new NamespacedKey(plugin, "iv_attack");
    ivDefenseKey = new NamespacedKey(plugin, "iv_defense");
    ivMagicKey = new NamespacedKey(plugin, "iv_magic");
  }

  @Override
  public Optional<PetRecord> load(@NonNull PersistentDataHolder holder) {
    return decodeFromParent(holder.getPersistentDataContainer());
  }

  @Override
  public void save(@NonNull PersistentDataHolder holder, @NonNull PetRecord record) {
    PersistentDataContainer parent = holder.getPersistentDataContainer();
    PersistentDataContainer container = encode(parent.getAdapterContext(), record);
    parent.set(rootKey, PersistentDataType.TAG_CONTAINER, container);
  }

  @Override
  public void clear(@NonNull PersistentDataHolder holder) {
    holder.getPersistentDataContainer().remove(rootKey);
  }

  @Override
  public boolean exists(@NonNull PersistentDataHolder holder) {
    return holder.getPersistentDataContainer().has(rootKey, PersistentDataType.TAG_CONTAINER);
  }

  @Override
  public Optional<PetRecord> compute(
      @NonNull PersistentDataHolder holder,
      @NonNull Function<Optional<PetRecord>, Optional<PetRecord>> operation) {
    PersistentDataContainer parent = holder.getPersistentDataContainer();
    Optional<PetRecord> current = decodeFromParent(parent);
    Optional<PetRecord> result = operation.apply(current);
    if (result == null) {
      throw new NullPointerException("Operation must not return null");
    }
    if (result.isPresent()) {
      save(holder, result.get());
    } else {
      clear(holder);
    }
    return result;
  }

  @Override
  public PersistentDataContainer encode(
      @NonNull PersistentDataAdapterContext context, @NonNull PetRecord record) {
    PersistentDataContainer container = context.newPersistentDataContainer();

    container.set(nameKey, PersistentDataType.STRING, record.displayName());
    container.set(generationKey, PersistentDataType.INTEGER, record.generation());
    container.set(breedKey, PersistentDataType.INTEGER, record.breedCount());
    container.set(talentsKey, PersistentDataType.STRING, String.join(";", record.talentIds()));
    container.set(mountKey, PersistentDataType.BYTE, asByte(record.mountUnlocked()));
    container.set(flightKey, PersistentDataType.BYTE, asByte(record.flightUnlocked()));

    container.set(evHealthKey, PersistentDataType.DOUBLE, record.evs().health());
    container.set(evAttackKey, PersistentDataType.DOUBLE, record.evs().attack());
    container.set(evDefenseKey, PersistentDataType.DOUBLE, record.evs().defense());
    container.set(evMagicKey, PersistentDataType.DOUBLE, record.evs().magic());

    container.set(ivHealthKey, PersistentDataType.DOUBLE, record.ivs().health());
    container.set(ivAttackKey, PersistentDataType.DOUBLE, record.ivs().attack());
    container.set(ivDefenseKey, PersistentDataType.DOUBLE, record.ivs().defense());
    container.set(ivMagicKey, PersistentDataType.DOUBLE, record.ivs().magic());

    return container;
  }

  @Override
  public Optional<PetRecord> decode(@NonNull PersistentDataContainer container) {
    return Option.of(container.get(nameKey, PersistentDataType.STRING))
        .filter(name -> !name.isBlank())
        .map(name -> buildRecord(container, name))
        .toJavaOptional();
  }

  private Optional<PetRecord> decodeFromParent(PersistentDataContainer parent) {
    return Option.of(parent.get(rootKey, PersistentDataType.TAG_CONTAINER))
        .flatMap(container -> Option.ofOptional(decode(container)))
        .toJavaOptional();
  }

  private PetRecord buildRecord(PersistentDataContainer container, String name) {
    List<String> talents =
        Option.of(container.get(talentsKey, PersistentDataType.STRING))
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
            getDouble(container, evHealthKey),
            getDouble(container, evAttackKey),
            getDouble(container, evDefenseKey),
            getDouble(container, evMagicKey));

    StatSet ivs =
        new StatSet(
            getDouble(container, ivHealthKey),
            getDouble(container, ivAttackKey),
            getDouble(container, ivDefenseKey),
            getDouble(container, ivMagicKey));

    int generation =
        Option.of(container.get(generationKey, PersistentDataType.INTEGER)).getOrElse(1);
    int breedCount = Option.of(container.get(breedKey, PersistentDataType.INTEGER)).getOrElse(0);
    boolean mountUnlocked =
        Option.of(container.get(mountKey, PersistentDataType.BYTE))
            .exists(value -> value == (byte) 1);
    boolean flightUnlocked =
        Option.of(container.get(flightKey, PersistentDataType.BYTE))
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
