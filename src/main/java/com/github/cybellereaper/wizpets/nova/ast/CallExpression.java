package com.github.cybellereaper.wizpets.nova.ast;

import java.util.List;
import java.util.Objects;

/** Function call expression. */
public record CallExpression(NovaExpression callee, List<NovaExpression> arguments)
    implements NovaExpression {
  public CallExpression {
    Objects.requireNonNull(callee, "callee");
    Objects.requireNonNull(arguments, "arguments");
    arguments = List.copyOf(arguments);
  }
}
