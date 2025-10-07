package com.github.cybellereaper.wizpets.nova.ast;

import java.util.List;
import java.util.Objects;

/** Pipe expression chains calls together. */
public record PipeExpression(NovaExpression seed, List<CallExpression> stages) implements NovaExpression {
  public PipeExpression {
    Objects.requireNonNull(seed, "seed");
    Objects.requireNonNull(stages, "stages");
    stages = List.copyOf(stages);
  }
}
