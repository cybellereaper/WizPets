package com.github.cybellereaper.noblepets.nova.ast;

import java.util.List;
import java.util.Objects;

/** Pattern matching expression. */
public record MatchExpression(NovaExpression target, List<MatchArm> arms) implements NovaExpression {
  public MatchExpression {
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(arms, "arms");
    if (arms.isEmpty()) {
      throw new IllegalArgumentException("match requires at least one arm");
    }
    arms = List.copyOf(arms);
  }
}
