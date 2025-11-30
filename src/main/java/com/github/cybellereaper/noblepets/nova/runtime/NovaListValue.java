package com.github.cybellereaper.noblepets.nova.runtime;

import java.util.List;
import java.util.Objects;

/** List runtime value. */
public record NovaListValue(List<NovaValue> elements) implements NovaValue {
  public NovaListValue {
    Objects.requireNonNull(elements, "elements");
    elements = List.copyOf(elements);
  }
}
