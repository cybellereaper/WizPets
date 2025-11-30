package com.github.cybellereaper.noblepets.nova.type;

/** Raised when type inference fails. */
public final class NovaTypeException extends RuntimeException {
  public NovaTypeException(String message) {
    super(message);
  }
}
