package com.github.cybellereaper.noblepets.nova.ast;

import java.util.List;
import java.util.Objects;

/** Lambda expression capturing parameters and body. */
public record LambdaExpression(List<Parameter> parameters, NovaExpression body)
    implements NovaExpression {
  public LambdaExpression {
    Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(body, "body");
    parameters = List.copyOf(parameters);
  }
}
