package com.github.cybellereaper.noblepets.core.model.blockbench;

import com.github.cybellereaper.noblepets.api.model.blockbench.BlockbenchBoneTarget;
import java.util.Map;

final class ModelData {
  private final String id;
  private final Map<String, BoneDefinition> bones;
  private final Map<String, BlockbenchAnimation> animations;

  ModelData(
      String id, Map<String, BoneDefinition> bones, Map<String, BlockbenchAnimation> animations) {
    this.id = id;
    this.bones = Map.copyOf(bones);
    this.animations = Map.copyOf(animations);
  }

  String id() {
    return id;
  }

  BoneDefinition bone(String name) {
    return bones.get(name);
  }

  BlockbenchAnimation animation(String name) {
    BlockbenchAnimation animation = animations.get(name);
    if (animation == null) {
      throw new IllegalArgumentException("Unknown animation '" + name + "' for model '" + id + "'");
    }
    return animation;
  }

  Map<String, BlockbenchAnimation> animations() {
    return animations;
  }

  record BoneDefinition(String name, BlockbenchBoneTarget target) {}
}
