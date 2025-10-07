package com.github.cybellereaper.wizpets.nova.ast;

import java.util.Objects;

/** Points to a type name declared in the type environment. */
public record TypeReference(String name) {
  public TypeReference {
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("type name cannot be blank");
    }
  }
}
