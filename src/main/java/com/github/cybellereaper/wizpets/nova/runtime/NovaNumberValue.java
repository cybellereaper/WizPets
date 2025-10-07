package com.github.cybellereaper.wizpets.nova.runtime;

import java.math.BigDecimal;
import java.util.Objects;

/** Numeric runtime value. */
public record NovaNumberValue(BigDecimal value) implements NovaValue {
  public NovaNumberValue {
    Objects.requireNonNull(value, "value");
  }

  public static NovaNumberValue of(double value) {
    return new NovaNumberValue(BigDecimal.valueOf(value));
  }
}
