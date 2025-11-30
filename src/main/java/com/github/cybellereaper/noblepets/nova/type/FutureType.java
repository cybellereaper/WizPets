package com.github.cybellereaper.noblepets.nova.type;

import java.util.Objects;

/** Represents the result of an async computation. */
public record FutureType(NovaType innerType) implements NovaType {
  public FutureType {
    Objects.requireNonNull(innerType, "innerType");
  }
}
