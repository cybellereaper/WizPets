package com.github.cybellereaper.wizpets.nova.ast;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Declares a top-level function. */
public record FunctionDeclaration(
    String name, List<Parameter> parameters, Optional<TypeReference> returnType, NovaExpression body)
    implements NovaDeclaration {
  public FunctionDeclaration {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(returnType, "returnType");
    Objects.requireNonNull(body, "body");
    if (name.isBlank()) {
      throw new IllegalArgumentException("function name cannot be blank");
    }
    parameters = List.copyOf(parameters);
  }
}
