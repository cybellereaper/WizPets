package com.github.cybellereaper.wizpets.core.model.blockbench;

import static org.mockito.Mockito.*;

import com.github.cybellereaper.wizpets.api.math.Vector3;
import com.github.cybellereaper.wizpets.api.model.blockbench.BlockbenchBoneTarget;
import com.github.cybellereaper.wizpets.api.model.blockbench.PoseTransform;
import java.util.Map;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.EulerAngle;
import org.junit.jupiter.api.Test;

class ArmorStandPoseTargetTest {
  @Test
  void appliesRotationAndResets() {
    ArmorStand stand = mock(ArmorStand.class);
    when(stand.getHeadPose()).thenReturn(new EulerAngle(0, 0, 0));
    when(stand.getBodyPose()).thenReturn(new EulerAngle(0, 0, 0));
    when(stand.getLeftArmPose()).thenReturn(new EulerAngle(0, 0, 0));
    when(stand.getRightArmPose()).thenReturn(new EulerAngle(0, 0, 0));
    when(stand.getLeftLegPose()).thenReturn(new EulerAngle(0, 0, 0));
    when(stand.getRightLegPose()).thenReturn(new EulerAngle(0, 0, 0));

    ArmorStandPoseTarget target = new ArmorStandPoseTarget(stand);

    target.apply(
        Map.of(
            BlockbenchBoneTarget.HEAD,
            new PoseTransform(new Vector3(90.0, 0.0, 0.0), Vector3.ZERO),
            BlockbenchBoneTarget.RIGHT_ARM,
            new PoseTransform(new Vector3(0.0, 45.0, 0.0), Vector3.ZERO)));

    verify(stand).setHeadPose(new EulerAngle(Math.toRadians(90.0), 0.0, 0.0));
    verify(stand).setRightArmPose(new EulerAngle(0.0, Math.toRadians(45.0), 0.0));

    target.reset();

    verify(stand, atLeastOnce()).setHeadPose(new EulerAngle(0.0, 0.0, 0.0));
    verify(stand, atLeastOnce()).setRightArmPose(new EulerAngle(0.0, 0.0, 0.0));
  }
}
