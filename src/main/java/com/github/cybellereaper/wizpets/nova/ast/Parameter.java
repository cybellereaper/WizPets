package com.github.cybellereaper.wizpets.nova.ast;

import java.util.Objects;
import java.util.Optional;

/** Represents a function or lambda parameter. */
public record Parameter(String name, Optional<TypeReference> type) {
  public Parameter {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    if (name.isBlank()) {
      throw new IllegalArgumentException("parameter name cannot be blank");
    }
  }

  public static Parameter untyped(String name) {
    return new Parameter(name, Optional.empty());
  }

  public static Parameter typed(String name, TypeReference type) {
    return new Parameter(name, Optional.of(type));
  }
}
