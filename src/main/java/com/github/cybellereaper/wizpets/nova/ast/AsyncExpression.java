package com.github.cybellereaper.wizpets.nova.ast;

import java.util.Objects;

/** Async expression wraps a block to run concurrently. */
public record AsyncExpression(BlockExpression block) implements NovaExpression {
  public AsyncExpression {
    Objects.requireNonNull(block, "block");
  }
}
