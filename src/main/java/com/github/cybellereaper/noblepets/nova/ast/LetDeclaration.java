package com.github.cybellereaper.noblepets.nova.ast;

import java.util.Objects;
import java.util.Optional;

/** Declares a top-level immutable value. */
public record LetDeclaration(String name, Optional<TypeReference> type, NovaExpression expression)
    implements NovaDeclaration {
  public LetDeclaration {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(expression, "expression");
    if (name.isBlank()) {
      throw new IllegalArgumentException("let name cannot be blank");
    }
  }
}
