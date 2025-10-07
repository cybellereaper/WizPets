package com.github.cybellereaper.wizpets.nova.runtime;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Simple adapter for exposing Java lambdas as Nova callables. */
public final class NovaHostFunction implements NovaCallable {
  private final Function<List<NovaValue>, NovaValue> delegate;

  public NovaHostFunction(Function<List<NovaValue>, NovaValue> delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public NovaValue invoke(List<NovaValue> arguments) {
    return delegate.apply(List.copyOf(arguments));
  }
}
