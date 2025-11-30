package com.github.cybellereaper.noblepets.nova.ast;

import java.util.List;
import java.util.Objects;

/** Represents a block expression. */
public record BlockExpression(List<NovaExpression> expressions) implements NovaExpression {
  public BlockExpression {
    Objects.requireNonNull(expressions, "expressions");
    expressions = List.copyOf(expressions);
  }
}
