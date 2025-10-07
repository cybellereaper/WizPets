package com.github.cybellereaper.wizpets.nova.runtime;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Represents an async computation. */
public record NovaFutureValue(CompletableFuture<NovaValue> future) implements NovaValue {
  public NovaFutureValue {
    Objects.requireNonNull(future, "future");
  }
}
