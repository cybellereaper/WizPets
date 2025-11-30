package com.github.cybellereaper.noblepets.nova.type;

import java.util.List;
import java.util.Objects;

/** Represents a user defined algebraic data type. */
public record UserType(String name, List<String> variants) implements NovaType {
  public UserType {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(variants, "variants");
    if (name.isBlank()) {
      throw new IllegalArgumentException("type name cannot be blank");
    }
    variants = List.copyOf(variants);
  }
}
