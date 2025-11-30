package com.github.cybellereaper.noblepets.nova.ast;

import java.util.List;
import java.util.Objects;

/** List literal expression. */
public record ListLiteralExpression(List<NovaExpression> elements) implements NovaExpression {
  public ListLiteralExpression {
    Objects.requireNonNull(elements, "elements");
    elements = List.copyOf(elements);
  }
}
