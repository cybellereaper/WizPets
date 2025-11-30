package com.github.cybellereaper.noblepets.core.model.blockbench;

import com.github.cybellereaper.noblepets.api.math.Vector3;
import com.github.cybellereaper.noblepets.api.model.blockbench.BlockbenchBoneTarget;
import com.github.cybellereaper.noblepets.api.model.blockbench.BlockbenchModelEngine;
import com.github.cybellereaper.noblepets.api.model.blockbench.BlockbenchModelInstance;
import com.github.cybellereaper.noblepets.api.model.blockbench.BlockbenchPoseTarget;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockbenchModelEngineImpl implements BlockbenchModelEngine {
  private final Gson gson = new GsonBuilder().create();
  private final Map<String, ModelData> models = new ConcurrentHashMap<>();

  @Override
  public void registerModel(String id, Reader reader) throws IOException {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(reader, "reader");
    String normalized = id.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Model id must not be blank");
    }
    FileSchema schema;
    try {
      schema = gson.fromJson(reader, FileSchema.class);
    } catch (JsonParseException ex) {
      throw new IOException("Unable to parse Blockbench model '" + id + "'", ex);
    }
    if (schema == null) {
      throw new IOException("Model definition for '" + id + "' was empty");
    }
    ModelData model = toModel(normalized, schema);
    models.put(normalized, model);
  }

  @Override
  public boolean hasModel(String id) {
    return models.containsKey(id);
  }

  @Override
  public Set<String> registeredModels() {
    return Collections.unmodifiableSet(models.keySet());
  }

  @Override
  public Set<String> animations(String id) {
    ModelData model = models.get(id);
    if (model == null) {
      return Set.of();
    }
    return Collections.unmodifiableSet(model.animations().keySet());
  }

  @Override
  public BlockbenchModelInstance createInstance(String id, BlockbenchPoseTarget target) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(target, "target");
    ModelData model = models.get(id);
    if (model == null) {
      throw new IllegalArgumentException("Unknown Blockbench model '" + id + "'");
    }
    return new BlockbenchModelInstanceImpl(model, target);
  }

  private ModelData toModel(String id, FileSchema schema) throws IOException {
    Map<String, ModelData.BoneDefinition> bones = new HashMap<>();
    if (schema.bones != null) {
      for (BoneSchema bone : schema.bones) {
        if (bone.name == null || bone.name.isBlank()) {
          throw new IOException("Encountered bone with no name in model '" + id + "'");
        }
        BlockbenchBoneTarget target = parseTarget(bone.target, id, bone.name);
        bones.put(bone.name, new ModelData.BoneDefinition(bone.name, target));
      }
    }

    Map<String, BlockbenchAnimation> animations = new HashMap<>();
    if (schema.animations != null) {
      for (Map.Entry<String, AnimationSchema> entry : schema.animations.entrySet()) {
        String animationId = entry.getKey();
        if (animationId == null || animationId.isBlank()) {
          continue;
        }
        animations.put(animationId, toAnimation(id, animationId, entry.getValue()));
      }
    }
    if (animations.isEmpty()) {
      throw new IOException("Model '" + id + "' does not define any animations");
    }
    return new ModelData(id, bones, animations);
  }

  private BlockbenchAnimation toAnimation(
      String modelId, String animationId, AnimationSchema schema) throws IOException {
    if (schema == null) {
      throw new IOException("Animation '" + animationId + "' for model '" + modelId + "' was null");
    }
    Map<String, BlockbenchBoneTrack> tracks = new HashMap<>();
    if (schema.bones != null) {
      for (Map.Entry<String, TrackSchema> entry : schema.bones.entrySet()) {
        String boneName = entry.getKey();
        TrackSchema trackSchema = entry.getValue();
        if (boneName == null || trackSchema == null) {
          continue;
        }
        tracks.put(boneName, toTrack(modelId, animationId, boneName, trackSchema));
      }
    }
    return new BlockbenchAnimation(schema.length, schema.loop, tracks);
  }

  private BlockbenchBoneTrack toTrack(
      String modelId, String animationId, String boneName, TrackSchema schema) throws IOException {
    List<BlockbenchKeyframe> rotation = new ArrayList<>();
    if (schema.rotation != null) {
      for (KeyframeSchema keyframe : schema.rotation) {
        if (keyframe == null) {
          continue;
        }
        rotation.add(
            new BlockbenchKeyframe(
                keyframe.time, toVector(modelId, animationId, boneName, keyframe)));
      }
      rotation.sort(java.util.Comparator.comparingDouble(BlockbenchKeyframe::time));
    }
    List<BlockbenchKeyframe> position = new ArrayList<>();
    if (schema.position != null) {
      for (KeyframeSchema keyframe : schema.position) {
        if (keyframe == null) {
          continue;
        }
        position.add(
            new BlockbenchKeyframe(
                keyframe.time, toVector(modelId, animationId, boneName, keyframe)));
      }
      position.sort(java.util.Comparator.comparingDouble(BlockbenchKeyframe::time));
    }
    return new BlockbenchBoneTrack(rotation, position);
  }

  private Vector3 toVector(
      String modelId, String animationId, String boneName, KeyframeSchema schema)
      throws IOException {
    List<Double> vector = schema.vector;
    if (vector == null || vector.size() != 3) {
      throw new IOException(
          "Invalid vector for bone '"
              + boneName
              + "' in animation '"
              + animationId
              + "' of model '"
              + modelId
              + "'");
    }
    return new Vector3(vector.get(0), vector.get(1), vector.get(2));
  }

  private BlockbenchBoneTarget parseTarget(String target, String modelId, String bone)
      throws IOException {
    if (target == null) {
      throw new IOException(
          "Bone '" + bone + "' in model '" + modelId + "' is missing a target mapping");
    }
    try {
      return BlockbenchBoneTarget.valueOf(target.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IOException(
          "Unsupported bone target '"
              + target
              + "' for bone '"
              + bone
              + "' in model '"
              + modelId
              + "'. Supported targets: "
              + EnumSet.allOf(BlockbenchBoneTarget.class),
          ex);
    }
  }

  private static final class FileSchema {
    List<BoneSchema> bones = List.of();
    Map<String, AnimationSchema> animations = Map.of();
  }

  private static final class BoneSchema {
    String name;
    String target;
  }

  private static final class AnimationSchema {
    double length;
    boolean loop;
    Map<String, TrackSchema> bones = Map.of();
  }

  private static final class TrackSchema {
    List<KeyframeSchema> rotation = List.of();
    List<KeyframeSchema> position = List.of();
  }

  private static final class KeyframeSchema {
    double time;
    List<Double> vector = List.of();
  }
}
