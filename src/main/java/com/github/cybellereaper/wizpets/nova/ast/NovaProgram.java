package com.github.cybellereaper.wizpets.nova.ast;

import java.util.List;
import java.util.Objects;

/** Represents a Nova module composed of declarations. */
public record NovaProgram(ModuleDeclaration module, List<NovaDeclaration> declarations) {
  public NovaProgram {
    Objects.requireNonNull(module, "module");
    Objects.requireNonNull(declarations, "declarations");
    declarations = List.copyOf(declarations);
  }
}
