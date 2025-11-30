package com.github.cybellereaper.noblepets.core.model.blockbench;

import com.github.cybellereaper.noblepets.api.math.Vector3;
import com.github.cybellereaper.noblepets.api.model.blockbench.PoseTransform;
import java.util.List;
import java.util.Optional;

final class BlockbenchBoneTrack {
  private final List<BlockbenchKeyframe> rotation;
  private final List<BlockbenchKeyframe> position;

  BlockbenchBoneTrack(List<BlockbenchKeyframe> rotation, List<BlockbenchKeyframe> position) {
    this.rotation = List.copyOf(rotation);
    this.position = List.copyOf(position);
  }

  PoseTransform poseAt(double time) {
    Vector3 rotationValue = rotationAt(time).orElse(Vector3.ZERO);
    Vector3 translationValue = positionAt(time).orElse(Vector3.ZERO);
    return new PoseTransform(rotationValue, translationValue);
  }

  private Optional<Vector3> rotationAt(double time) {
    return sample(rotation, time);
  }

  private Optional<Vector3> positionAt(double time) {
    return sample(position, time);
  }

  private Optional<Vector3> sample(List<BlockbenchKeyframe> frames, double time) {
    if (frames.isEmpty()) {
      return Optional.empty();
    }
    if (frames.size() == 1 || time <= frames.getFirst().time()) {
      return Optional.of(frames.getFirst().value());
    }
    BlockbenchKeyframe previous = frames.getFirst();
    for (int i = 1; i < frames.size(); i++) {
      BlockbenchKeyframe next = frames.get(i);
      if (time <= next.time()) {
        double span = next.time() - previous.time();
        if (span <= 0.0) {
          return Optional.of(next.value());
        }
        double factor = (time - previous.time()) / span;
        return Optional.of(previous.value().lerp(next.value(), factor));
      }
      previous = next;
    }
    return Optional.of(previous.value());
  }
}
