package com.github.cybellereaper.wizpets.nova.ast;

import java.util.Objects;

/** Effect expression for invoking host side effects. */
public record EffectExpression(NovaExpression expression) implements NovaExpression {
  public EffectExpression {
    Objects.requireNonNull(expression, "expression");
  }
}
