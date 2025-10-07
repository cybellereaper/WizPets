package com.github.cybellereaper.wizpets.nova.runtime;

import java.util.List;

/** Represents a callable Nova value. */
public non-sealed interface NovaCallable extends NovaValue {
  NovaValue invoke(List<NovaValue> arguments);
}
