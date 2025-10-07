package com.github.cybellereaper.wizpets.nova.type;

import java.util.Objects;

/** Represents a homogeneous list. */
public record ListType(NovaType elementType) implements NovaType {
  public ListType {
    Objects.requireNonNull(elementType, "elementType");
  }
}
