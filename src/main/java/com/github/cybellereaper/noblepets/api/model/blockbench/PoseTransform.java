package com.github.cybellereaper.noblepets.api.model.blockbench;

import com.github.cybellereaper.noblepets.api.math.Vector3;
import java.util.Objects;

/** Rotation and translation for a bone in degrees and Blockbench units. */
public record PoseTransform(Vector3 rotation, Vector3 translation) {
  public PoseTransform {
    Objects.requireNonNull(rotation, "rotation");
    Objects.requireNonNull(translation, "translation");
  }
}
