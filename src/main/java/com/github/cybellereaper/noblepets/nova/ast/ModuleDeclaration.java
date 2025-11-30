package com.github.cybellereaper.noblepets.nova.ast;

import java.util.List;
import java.util.Objects;

/** Declares the module path for a Nova program. */
public record ModuleDeclaration(List<String> pathSegments) {
  public ModuleDeclaration {
    Objects.requireNonNull(pathSegments, "pathSegments");
    if (pathSegments.isEmpty()) {
      throw new IllegalArgumentException("module path must have at least one segment");
    }
    pathSegments = List.copyOf(pathSegments);
  }

  public String qualifiedName() {
    return String.join(".", pathSegments);
  }
}
