package com.github.cybellereaper.noblepets.nova.runtime;

import java.util.List;
import java.util.Objects;

/** Represents an instance of a user defined variant. */
public record NovaVariantValue(String constructor, List<NovaValue> fields) implements NovaValue {
  public NovaVariantValue {
    Objects.requireNonNull(constructor, "constructor");
    Objects.requireNonNull(fields, "fields");
    fields = List.copyOf(fields);
  }
}
