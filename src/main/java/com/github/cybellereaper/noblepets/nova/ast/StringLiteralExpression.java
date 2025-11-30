package com.github.cybellereaper.noblepets.nova.ast;

import java.util.Objects;

/** String literal expression. */
public record StringLiteralExpression(String value) implements NovaExpression {
  public StringLiteralExpression {
    Objects.requireNonNull(value, "value");
  }
}
