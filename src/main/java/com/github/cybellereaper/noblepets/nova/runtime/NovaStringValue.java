package com.github.cybellereaper.noblepets.nova.runtime;

import java.util.Objects;

/** String runtime value. */
public record NovaStringValue(String value) implements NovaValue {
  public NovaStringValue {
    Objects.requireNonNull(value, "value");
  }
}
