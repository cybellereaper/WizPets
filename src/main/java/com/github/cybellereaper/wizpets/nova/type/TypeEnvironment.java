package com.github.cybellereaper.wizpets.nova.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Mutable type environment used during static analysis. */
public final class TypeEnvironment {
  private final TypeEnvironment parent;
  private final Map<String, NovaType> values = new HashMap<>();
  private final Map<String, NovaType> declaredTypes = new HashMap<>();

  public TypeEnvironment() {
    this(null);
  }

  public TypeEnvironment(TypeEnvironment parent) {
    this.parent = parent;
  }

  public TypeEnvironment createChild() {
    return new TypeEnvironment(this);
  }

  public Optional<NovaType> lookupValue(String name) {
    NovaType type = values.get(name);
    if (type != null) {
      return Optional.of(type);
    }
    if (parent != null) {
      return parent.lookupValue(name);
    }
    return Optional.empty();
  }

  public void defineValue(String name, NovaType type) {
    values.put(name, type);
  }

  public void defineType(String name, NovaType type) {
    declaredTypes.put(name, type);
  }

  public Optional<NovaType> lookupType(String name) {
    NovaType type = declaredTypes.get(name);
    if (type != null) {
      return Optional.of(type);
    }
    if (parent != null) {
      return parent.lookupType(name);
    }
    return Optional.empty();
  }
}
