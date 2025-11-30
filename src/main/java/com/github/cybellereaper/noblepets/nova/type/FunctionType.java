package com.github.cybellereaper.noblepets.nova.type;

import java.util.List;
import java.util.Objects;

/** Represents a function type. */
public record FunctionType(List<NovaType> parameters, NovaType returnType, boolean asynchronous)
    implements NovaType {
  public FunctionType {
    Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(returnType, "returnType");
    parameters = List.copyOf(parameters);
  }

  public static FunctionType sync(List<NovaType> parameters, NovaType returnType) {
    return new FunctionType(parameters, returnType, false);
  }

  public static FunctionType async(List<NovaType> parameters, NovaType returnType) {
    return new FunctionType(parameters, returnType, true);
  }
}
