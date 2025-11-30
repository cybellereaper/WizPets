package com.github.cybellereaper.noblepets.core.model.blockbench;

import com.github.cybellereaper.noblepets.api.model.blockbench.BlockbenchPoseTarget;

final class BlockbenchAnimationPlayer {
  private final BlockbenchAnimation animation;
  private final ModelData model;
  private double time;
  private boolean finished;

  BlockbenchAnimationPlayer(BlockbenchAnimation animation, ModelData model) {
    this.animation = animation;
    this.model = model;
  }

  void begin(BlockbenchPoseTarget target) {
    time = 0.0;
    finished = false;
    animation.apply(0.0, model, target);
  }

  void advance(double deltaSeconds, BlockbenchPoseTarget target) {
    if (finished) {
      return;
    }
    time += Math.max(0.0, deltaSeconds);
    double length = animation.length();
    if (length <= 0.0) {
      animation.apply(0.0, model, target);
      if (!animation.loop()) {
        finished = true;
      }
      return;
    }
    double sample = animation.loop() ? time % length : Math.min(time, length);
    animation.apply(sample, model, target);
    if (!animation.loop() && time >= length) {
      finished = true;
    }
  }

  boolean isFinished() {
    return finished;
  }

  boolean isLooping() {
    return animation.loop();
  }
}
