package com.github.cybellereaper.wizpets.core.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.api.StatType;
import com.github.cybellereaper.wizpets.testing.persistence.InMemoryPersistentDataContainer;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PetStorageTest {
  private JavaPlugin plugin;
  private Player player;
  private PetStorage storage;
  private InMemoryPersistentDataContainer container;

  @BeforeEach
  void setUp() {
    plugin = Mockito.mock(JavaPlugin.class);
    Mockito.when(plugin.getName()).thenReturn("wizpets-test");
    Mockito.when(plugin.namespace()).thenReturn("wizpets-test");
    storage = new PetStorage(plugin);

    container = new InMemoryPersistentDataContainer();
    player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(player.getName()).thenReturn("Althea");
    Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    Mockito.when(player.getPersistentDataContainer()).thenReturn(container);
  }

  @Test
  void saveAndLoadRoundTripPreservesState() {
    PetRecord original =
        new PetRecord(
            "Aurora",
            new StatSet(20.0, 15.5, 10.0, 12.5),
            new StatSet(10.0, 8.0, 6.0, 9.0),
            java.util.List.of("healing_aura", "guardian_shell"),
            3,
            2,
            true,
            true);

    storage.save(player, original);

    PetRecord loaded = storage.load(player).orElseThrow();
    assertEquals(original.displayName(), loaded.displayName());
    assertEquals(original.generation(), loaded.generation());
    assertEquals(original.breedCount(), loaded.breedCount());
    assertEquals(original.talentIds(), loaded.talentIds());
    assertEquals(original.mountUnlocked(), loaded.mountUnlocked());
    assertEquals(original.flightUnlocked(), loaded.flightUnlocked());

    for (StatType type : StatType.values()) {
      assertEquals(
          original.evs().value(type),
          loaded.evs().value(type),
          String.format(Locale.US, "Mismatch for %s EV", type));
      assertEquals(
          original.ivs().value(type),
          loaded.ivs().value(type),
          String.format(Locale.US, "Mismatch for %s IV", type));
    }
  }

  @Test
  void loadHandlesEmptyTalentList() {
    PetRecord record =
        new PetRecord(
            "Aurora",
            StatSet.randomEV(new java.util.Random(1)),
            StatSet.randomIV(new java.util.Random(2)),
            java.util.List.of(),
            1,
            0,
            false,
            false);

    storage.save(player, record);
    PetRecord loaded = storage.load(player).orElseThrow();
    assertTrue(loaded.talentIds().isEmpty());
  }

  @Test
  void encodeAndDecodeRoundTripMatchesRecord() {
    PetRecord original =
        new PetRecord(
            "Nebula",
            new StatSet(11.0, 9.5, 6.25, 4.75),
            new StatSet(3.0, 5.0, 7.0, 2.0),
            java.util.List.of("guardian_shell"),
            2,
            1,
            true,
            false);

    PetRecord decoded =
        storage.decode(storage.encode(container.getAdapterContext(), original)).orElseThrow();
    assertEquals(original, decoded);
  }

  @Test
  void decodeReturnsEmptyWhenNameMissing() {
    assertTrue(
        storage.decode(container.getAdapterContext().newPersistentDataContainer()).isEmpty());
  }

  @Test
  void clearRemovesStoredRootKey() {
    storage.save(player, baseline("Aurora"));
    storage.clear(player);

    NamespacedKey rootKey = new NamespacedKey(plugin, "pet");
    assertFalse(container.has(rootKey));
  }

  @Test
  void existsReflectsPresenceOfStoredRecord() {
    assertFalse(storage.exists(player));
    storage.save(player, baseline("Aurora"));
    assertTrue(storage.exists(player));
  }

  @Test
  void loadOrCreatePersistsGeneratedRecordOnce() {
    AtomicInteger invocations = new AtomicInteger();
    PetRecord generated = baseline("Aurora");

    PetRecord first =
        storage.loadOrCreate(
            player,
            () -> {
              invocations.incrementAndGet();
              return generated;
            });
    assertEquals(1, invocations.get());
    assertEquals(generated, first);

    PetRecord second =
        storage.loadOrCreate(
            player,
            () -> {
              invocations.incrementAndGet();
              return baseline("Should not be used");
            });

    assertEquals(1, invocations.get());
    assertEquals(generated, second);
  }

  @Test
  void computeSupportsAtomicCreationAndRemoval() {
    Optional<PetRecord> created =
        storage.compute(
            player,
            existing -> {
              assertTrue(existing.isEmpty());
              return Optional.of(baseline("Aurora"));
            });

    assertTrue(created.isPresent());
    assertTrue(storage.exists(player));

    Optional<PetRecord> removed =
        storage.compute(
            player,
            existing -> {
              assertTrue(existing.isPresent());
              return Optional.empty();
            });

    assertTrue(removed.isEmpty());
    assertFalse(storage.exists(player));
  }

  @Test
  void computeUpdatesExistingRecordUsingLatestState() {
    storage.save(player, baseline("Aurora"));

    Optional<PetRecord> updated =
        storage.compute(
            player,
            existing -> {
              assertEquals("Aurora", existing.orElseThrow().displayName());
              return existing.map(record -> record.withDisplayName("Nebula"));
            });

    assertEquals("Nebula", updated.orElseThrow().displayName());
    assertEquals("Nebula", storage.load(player).orElseThrow().displayName());
  }

  private PetRecord baseline(String name) {
    return new PetRecord(
        name,
        new StatSet(20.0, 15.5, 10.0, 12.5),
        new StatSet(10.0, 8.0, 6.0, 9.0),
        java.util.List.of("healing_aura"),
        3,
        2,
        true,
        true);
  }
}
