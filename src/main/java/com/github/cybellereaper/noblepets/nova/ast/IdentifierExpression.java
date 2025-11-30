package com.github.cybellereaper.noblepets.nova.ast;

import java.util.Objects;

/** Reference to a value in the current environment. */
public record IdentifierExpression(String name) implements NovaExpression {
  public IdentifierExpression {
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("identifier cannot be blank");
    }
  }
}
