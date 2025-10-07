package com.github.cybellereaper.wizpets.nova.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Mutable environment for runtime evaluation. */
public final class NovaEnvironment {
  private final NovaEnvironment parent;
  private final Map<String, NovaValue> values = new HashMap<>();

  public NovaEnvironment() {
    this(null);
  }

  public NovaEnvironment(NovaEnvironment parent) {
    this.parent = parent;
  }

  public NovaEnvironment createChild() {
    return new NovaEnvironment(this);
  }

  public void define(String name, NovaValue value) {
    values.put(name, value);
  }

  public Optional<NovaValue> lookup(String name) {
    NovaValue value = values.get(name);
    if (value != null) {
      return Optional.of(value);
    }
    if (parent != null) {
      return parent.lookup(name);
    }
    return Optional.empty();
  }
}
