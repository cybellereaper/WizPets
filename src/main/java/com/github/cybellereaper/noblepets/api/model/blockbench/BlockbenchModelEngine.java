package com.github.cybellereaper.noblepets.api.model.blockbench;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

/** Facade for registering Blockbench models and creating animators. */
public interface BlockbenchModelEngine {
  void registerModel(String id, Reader reader) throws IOException;

  default void registerModel(String id, InputStream stream) throws IOException {
    Objects.requireNonNull(stream, "stream");
    try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      registerModel(id, reader);
    }
  }

  boolean hasModel(String id);

  Set<String> registeredModels();

  Set<String> animations(String id);

  BlockbenchModelInstance createInstance(String id, BlockbenchPoseTarget target);
}
