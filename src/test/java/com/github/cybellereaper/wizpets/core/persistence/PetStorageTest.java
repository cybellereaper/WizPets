package com.github.cybellereaper.wizpets.core.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cybellereaper.wizpets.api.PetRecord;
import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.api.StatType;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PetStorageTest {
  private Player player;
  private PetStorage storage;
  private InMemoryContainer container;

  @BeforeEach
  void setUp() {
    JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
    Mockito.when(plugin.getName()).thenReturn("wizpets-test");
    Mockito.when(plugin.namespace()).thenReturn("wizpets-test");
    storage = new PetStorage(plugin);

    container = new InMemoryContainer();
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

  private static final class InMemoryContainer implements PersistentDataContainer {
    private final Map<NamespacedKey, Object> store = new HashMap<>();

    @Override
    public <P, C> void set(NamespacedKey key, PersistentDataType<P, C> type, C value) {
      store.put(key, value);
    }

    @Override
    public void remove(NamespacedKey key) {
      store.remove(key);
    }

    @Override
    public void readFromBytes(byte[] bytes, boolean clear) throws IOException {
      throw new IOException("Not supported in tests");
    }

    @Override
    public <P, C> boolean has(NamespacedKey key, PersistentDataType<P, C> type) {
      return store.containsKey(key);
    }

    @Override
    public boolean has(NamespacedKey key) {
      return store.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P, C> C get(NamespacedKey key, PersistentDataType<P, C> type) {
      return (C) store.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P, C> C getOrDefault(NamespacedKey key, PersistentDataType<P, C> type, C defaultValue) {
      return (C) store.getOrDefault(key, defaultValue);
    }

    @Override
    public Set<NamespacedKey> getKeys() {
      return new HashSet<>(store.keySet());
    }

    @Override
    public boolean isEmpty() {
      return store.isEmpty();
    }

    @Override
    public void copyTo(PersistentDataContainer other, boolean replace) {
      if (!(other instanceof InMemoryContainer target)) {
        throw new UnsupportedOperationException("Unsupported container type");
      }
      if (replace) {
        target.store.clear();
      }
      target.store.putAll(store);
    }

    @Override
    public PersistentDataAdapterContext getAdapterContext() {
      return () -> new InMemoryContainer();
    }

    @Override
    public byte[] serializeToBytes() throws IOException {
      throw new IOException("Not supported in tests");
    }
  }
}
