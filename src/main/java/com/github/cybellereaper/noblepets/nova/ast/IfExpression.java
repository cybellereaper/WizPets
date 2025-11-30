package com.github.cybellereaper.noblepets.nova.ast;

import java.util.Objects;
import java.util.Optional;

/** If/else expression. */
public record IfExpression(
    NovaExpression condition, NovaExpression thenBranch, Optional<NovaExpression> elseBranch)
    implements NovaExpression {
  public IfExpression {
    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(thenBranch, "thenBranch");
    Objects.requireNonNull(elseBranch, "elseBranch");
  }
}
