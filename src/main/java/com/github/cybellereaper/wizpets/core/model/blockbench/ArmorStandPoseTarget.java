package com.github.cybellereaper.wizpets.core.model.blockbench;

import com.github.cybellereaper.wizpets.api.math.Vector3;
import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchBoneTarget;
import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchPoseTarget;
import com.github.cybellereaper.wizpets.api.model.blockbench.PoseTransform;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.EulerAngle;

public final class ArmorStandPoseTarget implements BlockbenchPoseTarget {
  private final ArmorStand stand;
  private final Map<BlockbenchBoneTarget, EulerAngle> baseline =
      new EnumMap<>(BlockbenchBoneTarget.class);

  public ArmorStandPoseTarget(ArmorStand stand) {
    this.stand = Objects.requireNonNull(stand, "stand");
    baseline.put(BlockbenchBoneTarget.HEAD, stand.getHeadPose());
    baseline.put(BlockbenchBoneTarget.BODY, stand.getBodyPose());
    baseline.put(BlockbenchBoneTarget.LEFT_ARM, stand.getLeftArmPose());
    baseline.put(BlockbenchBoneTarget.RIGHT_ARM, stand.getRightArmPose());
    baseline.put(BlockbenchBoneTarget.LEFT_LEG, stand.getLeftLegPose());
    baseline.put(BlockbenchBoneTarget.RIGHT_LEG, stand.getRightLegPose());
  }

  @Override
  public void apply(Map<BlockbenchBoneTarget, PoseTransform> pose) {
    pose.forEach((target, transform) -> applyRotation(target, transform.rotation()));
  }

  @Override
  public void reset() {
    baseline.forEach(this::applyEuler);
  }

  private void applyRotation(BlockbenchBoneTarget target, Vector3 rotationDegrees) {
    EulerAngle angle = toEuler(rotationDegrees);
    switch (target) {
      case HEAD -> stand.setHeadPose(angle);
      case BODY -> stand.setBodyPose(angle);
      case LEFT_ARM -> stand.setLeftArmPose(angle);
      case RIGHT_ARM -> stand.setRightArmPose(angle);
      case LEFT_LEG -> stand.setLeftLegPose(angle);
      case RIGHT_LEG -> stand.setRightLegPose(angle);
    }
  }

  private void applyEuler(BlockbenchBoneTarget target, EulerAngle angle) {
    switch (target) {
      case HEAD -> stand.setHeadPose(angle);
      case BODY -> stand.setBodyPose(angle);
      case LEFT_ARM -> stand.setLeftArmPose(angle);
      case RIGHT_ARM -> stand.setRightArmPose(angle);
      case LEFT_LEG -> stand.setLeftLegPose(angle);
      case RIGHT_LEG -> stand.setRightLegPose(angle);
    }
  }

  private EulerAngle toEuler(Vector3 rotationDegrees) {
    return new EulerAngle(
        Math.toRadians(rotationDegrees.x()),
        Math.toRadians(rotationDegrees.y()),
        Math.toRadians(rotationDegrees.z()));
  }
}
