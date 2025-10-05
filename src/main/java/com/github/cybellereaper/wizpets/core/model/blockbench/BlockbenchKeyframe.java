package com.github.cybellereaper.wizpets.core.model.blockbench;

import com.github.cybellereaper.wizpets.api.math.Vector3;

record BlockbenchKeyframe(double time, Vector3 value) {
  BlockbenchKeyframe {
    if (time < 0.0) {
      throw new IllegalArgumentException("time must be non-negative");
    }
  }
}
