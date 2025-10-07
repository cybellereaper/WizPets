package com.github.cybellereaper.wizpets.nova.runtime;

/** Raised when evaluation encounters an error. */
public final class NovaEvaluationException extends RuntimeException {
  public NovaEvaluationException(String message) {
    super(message);
  }

  public NovaEvaluationException(String message, Throwable cause) {
    super(message, cause);
  }
}
