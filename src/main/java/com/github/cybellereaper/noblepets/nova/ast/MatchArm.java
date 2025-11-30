package com.github.cybellereaper.noblepets.nova.ast;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Represents a single arm of a match expression. */
public record MatchArm(String constructor, Optional<List<Parameter>> destructured, NovaExpression body) {
  public MatchArm {
    Objects.requireNonNull(constructor, "constructor");
    Objects.requireNonNull(destructured, "destructured");
    Objects.requireNonNull(body, "body");
    if (constructor.isBlank()) {
      throw new IllegalArgumentException("constructor cannot be blank");
    }
    destructured = destructured.map(List::copyOf);
  }
}
