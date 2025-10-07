package com.github.cybellereaper.wizpets.nova.ast;

import java.util.Objects;

/** Await expression waits for an async computation. */
public record AwaitExpression(NovaExpression expression) implements NovaExpression {
  public AwaitExpression {
    Objects.requireNonNull(expression, "expression");
  }
}
