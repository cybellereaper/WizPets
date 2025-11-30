package com.github.cybellereaper.noblepets.nova.runtime;

import java.util.Objects;

/** Wraps a raw Java object to expose it to Nova scripts. */
public record NovaJavaValue(Object value) implements NovaValue {
  public NovaJavaValue {
    Objects.requireNonNull(value, "value");
  }
}
