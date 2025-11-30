package com.github.cybellereaper.noblepets.api.model.blockbench;

import java.util.Map;

/** Consumer of Blockbench bone poses such as armor stands or fake renderers. */
public interface BlockbenchPoseTarget {
  void apply(Map<BlockbenchBoneTarget, PoseTransform> pose);

  default void reset() {}
}
