package com.github.cybellereaper.wizpets.core.model.blockbench;

import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchModelInstance;
import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchPoseTarget;
import java.util.Objects;
import java.util.Optional;

final class BlockbenchModelInstanceImpl implements BlockbenchModelInstance {
  private final ModelData model;
  private final BlockbenchPoseTarget target;
  private BlockbenchAnimationPlayer active;
  private BlockbenchAnimationPlayer loop;
  private BlockbenchAnimationPlayer pendingLoop;
  private String activeName;
  private String loopName;
  private boolean destroyed;

  BlockbenchModelInstanceImpl(ModelData model, BlockbenchPoseTarget target) {
    this.model = Objects.requireNonNull(model, "model");
    this.target = Objects.requireNonNull(target, "target");
  }

  @Override
  public String modelId() {
    return model.id();
  }

  @Override
  public Optional<String> currentAnimation() {
    return Optional.ofNullable(activeName);
  }

  @Override
  public void playLoop(String animationName) {
    ensureActive();
    BlockbenchAnimation animation = model.animation(animationName);
    BlockbenchAnimationPlayer player = new BlockbenchAnimationPlayer(animation, model);
    loop = player;
    loopName = animationName;
    if (active == null || active == loop || active.isFinished()) {
      active = player;
      activeName = animationName;
      player.begin(target);
    } else {
      pendingLoop = player;
    }
  }

  @Override
  public void playOnce(String animationName) {
    ensureActive();
    BlockbenchAnimation animation = model.animation(animationName);
    BlockbenchAnimationPlayer player = new BlockbenchAnimationPlayer(animation, model);
    active = player;
    activeName = animationName;
    pendingLoop = loop;
    player.begin(target);
  }

  @Override
  public void tick(double deltaSeconds) {
    if (destroyed || active == null) {
      return;
    }
    active.advance(deltaSeconds, target);
    if (active.isFinished()) {
      if (pendingLoop != null) {
        active = pendingLoop;
        activeName = loopName;
        pendingLoop = null;
        if (active != null) {
          active.begin(target);
        }
      } else if (loop != null) {
        active = loop;
        activeName = loopName;
        active.begin(target);
      } else {
        active = null;
        activeName = null;
      }
    }
  }

  @Override
  public void destroy() {
    if (destroyed) {
      return;
    }
    destroyed = true;
    active = null;
    loop = null;
    pendingLoop = null;
    activeName = null;
    loopName = null;
    target.reset();
  }

  private void ensureActive() {
    if (destroyed) {
      throw new IllegalStateException("Model instance destroyed");
    }
  }
}
