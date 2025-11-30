package com.github.cybellereaper.noblepets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cybellereaper.noblepets.api.StatSet;
import com.github.cybellereaper.noblepets.api.StatType;
import java.util.EnumSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

class StatSetTest {
  @Test
  void breedWithMixesStatsFromParents() {
    StatSet parentA = new StatSet(40.0, 10.0, 12.0, 8.0);
    StatSet parentB = new StatSet(30.0, 18.0, 9.0, 11.0);
    StatSet child = parentA.breedWith(parentB, new Random(42));

    for (StatType type : StatType.values()) {
      double min = Math.min(parentA.value(type), parentB.value(type)) - 3.0;
      double max = Math.max(parentA.value(type), parentB.value(type)) + 3.0;
      double actual = child.value(type);
      assertTrue(actual >= min && actual <= max, type + " should be within blended bounds");
    }
  }

  @Test
  void randomGeneratorsProduceVariedResults() {
    StatSet sampleA = StatSet.randomEV(new Random(1));
    StatSet sampleB = StatSet.randomEV(new Random(2));
    assertNotEquals(sampleA, sampleB);
    for (StatType type : StatType.values()) {
      assertTrue(sampleA.value(type) >= 0.0);
    }
  }

  @Test
  void updateReturnsNewInstanceWithAdjustedValue() {
    StatSet base = new StatSet(10.0, 10.0, 10.0, 10.0);
    StatSet updated = base.with(StatType.MAGIC, 42.0);
    assertEquals(42.0, updated.magic());
    assertEquals(10.0, base.magic());
    for (StatType type : EnumSet.complementOf(EnumSet.of(StatType.MAGIC))) {
      assertEquals(base.value(type), updated.value(type));
    }
  }

  @Test
  void asMapProducesImmutableSnapshot() {
    StatSet stats = new StatSet(5.0, 6.0, 7.0, 8.0);
    var map = stats.asMap();
    assertEquals(4, map.size());
    for (StatType type : StatType.values()) {
      assertEquals(stats.value(type), map.get(type));
    }
    assertThrows(UnsupportedOperationException.class, () -> map.put(StatType.ATTACK, 9.0));
  }

  @Test
  void constructorRejectsInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> new StatSet(-1.0, 0.0, 0.0, 0.0));
    assertThrows(IllegalArgumentException.class, () -> new StatSet(Double.NaN, 0.0, 0.0, 0.0));
  }
}
