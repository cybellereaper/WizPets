package com.github.cybellereaper.wizpets.nova.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    Objects.requireNonNull(name, "name");
    return lookupInChain(name, true);
  }

  public void defineValue(String name, NovaType type) {
    values.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(type, "type"));
  }

  public void defineType(String name, NovaType type) {
    declaredTypes.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(type, "type"));
  }

  public Optional<NovaType> lookupType(String name) {
    Objects.requireNonNull(name, "name");
    return lookupInChain(name, false);
  }

  private Optional<NovaType> lookupInChain(String name, boolean searchValues) {
    for (TypeEnvironment current = this; current != null; current = current.parent) {
      Map<String, NovaType> scope = searchValues ? current.values : current.declaredTypes;
      NovaType found = scope.get(name);
      if (found != null) {
        return Optional.of(found);
      }
    }
    return Optional.empty();
  }
}
