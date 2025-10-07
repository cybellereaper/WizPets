package com.github.cybellereaper.wizpets.nova.ast;

import java.math.BigDecimal;
import java.util.Objects;

/** Numeric literal expression. */
public record NumberLiteralExpression(BigDecimal value) implements NovaExpression {
  public NumberLiteralExpression {
    Objects.requireNonNull(value, "value");
  }

  public static NumberLiteralExpression of(double value) {
    return new NumberLiteralExpression(BigDecimal.valueOf(value));
  }
}
