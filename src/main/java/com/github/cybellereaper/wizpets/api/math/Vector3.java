package com.github.cybellereaper.wizpets.api.math;

import java.util.Objects;

/** Immutable three dimensional vector using double precision. */
public record Vector3(double x, double y, double z) {
  public static final Vector3 ZERO = new Vector3(0.0, 0.0, 0.0);

  public Vector3 {
    if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
      throw new IllegalArgumentException("Vector components must not be NaN");
    }
    if (Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
      throw new IllegalArgumentException("Vector components must be finite");
    }
  }

  public Vector3 add(Vector3 other) {
    Objects.requireNonNull(other, "other");
    return new Vector3(x + other.x, y + other.y, z + other.z);
  }

  public Vector3 subtract(Vector3 other) {
    Objects.requireNonNull(other, "other");
    return new Vector3(x - other.x, y - other.y, z - other.z);
  }

  public Vector3 scale(double scalar) {
    if (Double.isNaN(scalar) || Double.isInfinite(scalar)) {
      throw new IllegalArgumentException("Scalar must be finite");
    }
    return new Vector3(x * scalar, y * scalar, z * scalar);
  }

  public Vector3 lerp(Vector3 other, double t) {
    Objects.requireNonNull(other, "other");
    double clamped = Math.max(0.0, Math.min(1.0, t));
    return new Vector3(
        x + (other.x - x) * clamped,
        y + (other.y - y) * clamped,
        z + (other.z - z) * clamped);
  }
}
