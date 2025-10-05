package com.github.cybellereaper.wizpets;

import com.github.cybellereaper.wizpets.api.StatSet;
import com.github.cybellereaper.wizpets.api.StatType;
import java.util.EnumSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        StatSet updated = base.update(StatType.MAGIC, 42.0);
        assertEquals(42.0, updated.getMagic());
        assertEquals(10.0, base.getMagic());
        for (StatType type : EnumSet.complementOf(EnumSet.of(StatType.MAGIC))) {
            assertEquals(base.value(type), updated.value(type));
        }
    }
}
