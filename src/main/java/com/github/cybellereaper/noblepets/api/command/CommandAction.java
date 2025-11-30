package com.github.cybellereaper.noblepets.api.command;

import java.util.List;

/** Functional contract for executable sub-commands within the NoblePets command tree. */
@FunctionalInterface
public interface CommandAction {
  /** Executes the action for the given context, returning whether execution succeeded. */
  boolean execute(CommandContext context);

  /** Provides tab-completion suggestions for the action when applicable. */
  default List<String> tabComplete(CommandContext context) {
    return List.of();
  }
}
