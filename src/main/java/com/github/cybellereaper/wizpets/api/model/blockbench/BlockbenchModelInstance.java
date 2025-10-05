package com.github.cybellereaper.wizpets.api.model.blockbench;

import java.util.Optional;

/** Runtime instance controlling animation playback for a model. */
public interface BlockbenchModelInstance {
  String modelId();

  Optional<String> currentAnimation();

  void playLoop(String animationName);

  void playOnce(String animationName);

  void tick(double deltaSeconds);

  void destroy();
}
