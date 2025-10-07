package com.github.cybellereaper.wizpets.nova.runtime;

import java.util.Objects;

/** String runtime value. */
public record NovaStringValue(String value) implements NovaValue {
  public NovaStringValue {
    Objects.requireNonNull(value, "value");
  }
}
