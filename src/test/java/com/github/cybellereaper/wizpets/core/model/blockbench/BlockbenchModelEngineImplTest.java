package com.github.cybellereaper.wizpets.core.model.blockbench;

import static org.junit.jupiter.api.Assertions.*;

import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchBoneTarget;
import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchModelEngine;
import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchModelInstance;
import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchPoseTarget;
import com.github.cybellereaper.wizpets.api.model.blockbench.PoseTransform;
import java.io.StringReader;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockbenchModelEngineImplTest {
  private static final String MODEL_JSON =
      """
          {
            "bones": [
              {"name": "body", "target": "BODY"},
              {"name": "head", "target": "HEAD"}
            ],
            "animations": {
              "idle": {
                "length": 2.0,
                "loop": true,
                "bones": {
                  "head": {
                    "rotation": [
                      {"time": 0.0, "vector": [0, 0, 0]},
                      {"time": 1.0, "vector": [10, 0, 0]},
                      {"time": 2.0, "vector": [0, 0, 0]}
                    ]
                  }
                }
              },
              "attack": {
                "length": 0.8,
                "loop": false,
                "bones": {
                  "head": {
                    "rotation": [
                      {"time": 0.0, "vector": [0, 0, 0]},
                      {"time": 0.4, "vector": [30, 0, 0]},
                      {"time": 0.8, "vector": [0, 0, 0]}
                    ]
                  }
                }
              }
            }
          }
          """;

  private BlockbenchModelEngine engine;

  @BeforeEach
  void setUp() throws Exception {
    engine = new BlockbenchModelEngineImpl();
    engine.registerModel("test", new StringReader(MODEL_JSON));
  }

  @Test
  void registersAndCreatesInstance() {
    assertTrue(engine.hasModel("test"));
    assertEquals(Set.of("idle", "attack"), engine.animations("test"));

    RecordingTarget target = new RecordingTarget();
    BlockbenchModelInstance instance = engine.createInstance("test", target);
    assertEquals("test", instance.modelId());
    assertEquals(Optional.empty(), instance.currentAnimation());

    instance.playLoop("idle");
    assertEquals(Optional.of("idle"), instance.currentAnimation());
    PoseTransform initial = target.get(BlockbenchBoneTarget.HEAD);
    assertNotNull(initial);
    assertEquals(0.0, initial.rotation().x(), 1.0e-6);

    instance.tick(1.0);
    PoseTransform mid = target.get(BlockbenchBoneTarget.HEAD);
    assertNotNull(mid);
    assertEquals(10.0, mid.rotation().x(), 1.0e-6);
  }

  @Test
  void playsAttackAndReturnsToIdle() {
    RecordingTarget target = new RecordingTarget();
    BlockbenchModelInstance instance = engine.createInstance("test", target);
    instance.playLoop("idle");

    instance.playOnce("attack");
    instance.tick(0.4);
    PoseTransform peak = target.get(BlockbenchBoneTarget.HEAD);
    assertNotNull(peak);
    assertEquals(30.0, peak.rotation().x(), 1.0e-6);

    instance.tick(0.5);
    instance.tick(0.1);
    PoseTransform reset = target.get(BlockbenchBoneTarget.HEAD);
    assertNotNull(reset);
    assertTrue(reset.rotation().x() <= 10.0);
    assertEquals(Optional.of("idle"), instance.currentAnimation());
  }

  private static final class RecordingTarget implements BlockbenchPoseTarget {
    private Map<BlockbenchBoneTarget, PoseTransform> last = Map.of();

    @Override
    public void apply(Map<BlockbenchBoneTarget, PoseTransform> pose) {
      last = new EnumMap<>(pose);
    }

    PoseTransform get(BlockbenchBoneTarget target) {
      return last.get(target);
    }
  }
}
