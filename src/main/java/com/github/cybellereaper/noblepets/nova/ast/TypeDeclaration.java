package com.github.cybellereaper.noblepets.nova.ast;

import java.util.List;
import java.util.Objects;

/** Declares an algebraic data type or alias. */
public record TypeDeclaration(String name, List<String> variants) implements NovaDeclaration {
  public TypeDeclaration {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(variants, "variants");
    if (name.isBlank()) {
      throw new IllegalArgumentException("type name cannot be blank");
    }
    variants = List.copyOf(variants);
  }
}
