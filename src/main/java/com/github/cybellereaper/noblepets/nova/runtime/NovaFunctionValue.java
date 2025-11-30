package com.github.cybellereaper.noblepets.nova.runtime;

import com.github.cybellereaper.noblepets.nova.ast.NovaExpression;
import com.github.cybellereaper.noblepets.nova.ast.Parameter;
import java.util.List;
import java.util.Objects;

/** Represents a user defined Nova function. */
public final class NovaFunctionValue implements NovaCallable {
  private final List<Parameter> parameters;
  private final NovaExpression body;
  private final NovaEnvironment closure;
  private final NovaEvaluator evaluator;

  public NovaFunctionValue(
      List<Parameter> parameters, NovaExpression body, NovaEnvironment closure, NovaEvaluator evaluator) {
    this.parameters = List.copyOf(parameters);
    this.body = Objects.requireNonNull(body, "body");
    this.closure = Objects.requireNonNull(closure, "closure");
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
  }

  public List<Parameter> parameters() {
    return parameters;
  }

  public NovaExpression body() {
    return body;
  }

  public NovaEnvironment closure() {
    return closure;
  }

  @Override
  public NovaValue invoke(List<NovaValue> arguments) {
    return evaluator.invokeFunction(this, arguments);
  }
}
