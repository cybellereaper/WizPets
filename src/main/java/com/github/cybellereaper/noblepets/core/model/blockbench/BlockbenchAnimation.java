package com.github.cybellereaper.noblepets.core.model.blockbench;

import com.github.cybellereaper.noblepets.api.model.blockbench.BlockbenchBoneTarget;
import com.github.cybellereaper.noblepets.api.model.blockbench.BlockbenchPoseTarget;
import com.github.cybellereaper.noblepets.api.model.blockbench.PoseTransform;
import java.util.EnumMap;
import java.util.Map;

final class BlockbenchAnimation {
  private final double length;
  private final boolean loop;
  private final Map<String, BlockbenchBoneTrack> tracks;

  BlockbenchAnimation(double length, boolean loop, Map<String, BlockbenchBoneTrack> tracks) {
    this.length = Math.max(0.0, length);
    this.loop = loop;
    this.tracks = Map.copyOf(tracks);
  }

  double length() {
    return length;
  }

  boolean loop() {
    return loop;
  }

  void apply(double time, ModelData model, BlockbenchPoseTarget target) {
    Map<BlockbenchBoneTarget, PoseTransform> pose = new EnumMap<>(BlockbenchBoneTarget.class);
    tracks.forEach(
        (bone, track) -> {
          ModelData.BoneDefinition definition = model.bone(bone);
          if (definition == null) {
            return;
          }
          pose.put(definition.target(), track.poseAt(time));
        });
    if (!pose.isEmpty()) {
      target.apply(pose);
    }
  }
}
