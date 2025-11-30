package com.github.cybellereaper.noblepets.testing.persistence;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Simple in-memory implementation of {@link PersistentDataContainer} for tests. */
public final class InMemoryPersistentDataContainer implements PersistentDataContainer {
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
    if (!(other instanceof InMemoryPersistentDataContainer target)) {
      throw new UnsupportedOperationException("Unsupported container type");
    }
    if (replace) {
      target.store.clear();
    }
    target.store.putAll(store);
  }

  @Override
  public PersistentDataAdapterContext getAdapterContext() {
    return () -> new InMemoryPersistentDataContainer();
  }

  @Override
  public byte[] serializeToBytes() throws IOException {
    throw new IOException("Not supported in tests");
  }
}
