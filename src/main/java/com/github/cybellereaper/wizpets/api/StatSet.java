package com.github.cybellereaper.wizpets.api;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Immutable set of pet battle statistics.
 */
public record StatSet(double health, double attack, double defense, double magic) {
    private static final double MIN_MIXED_VALUE = 1.0;

    public StatSet {
        validateScalar("health", health);
        validateScalar("attack", attack);
        validateScalar("defense", defense);
        validateScalar("magic", magic);
    }

    public double value(StatType type) {
        return switch (type) {
            case HEALTH -> health;
            case ATTACK -> attack;
            case DEFENSE -> defense;
            case MAGIC -> magic;
        };
    }

    public StatSet with(StatType type, double value) {
        return switch (type) {
            case HEALTH -> new StatSet(value, attack, defense, magic);
            case ATTACK -> new StatSet(health, value, defense, magic);
            case DEFENSE -> new StatSet(health, attack, value, magic);
            case MAGIC -> new StatSet(health, attack, defense, value);
        };
    }

    public StatSet breedWith(StatSet other, RandomGenerator random) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(random, "random");
        return new StatSet(
            mix(health, other.health, random),
            mix(attack, other.attack, random),
            mix(defense, other.defense, random),
            mix(magic, other.magic, random)
        );
    }

    private static double mix(double a, double b, RandomGenerator random) {
        double base = (a + b) / 2.0;
        double variation = random.nextDouble(-2.75, 2.75);
        return Math.max(MIN_MIXED_VALUE, base + variation);
    }

    public static StatSet randomEV(RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        return new StatSet(
            random.nextDouble(0.0, 64.0),
            random.nextDouble(0.0, 64.0),
            random.nextDouble(0.0, 64.0),
            random.nextDouble(0.0, 64.0)
        );
    }

    public static StatSet randomIV(RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        return new StatSet(
            random.nextDouble(0.0, 15.0),
            random.nextDouble(0.0, 15.0),
            random.nextDouble(0.0, 15.0),
            random.nextDouble(0.0, 15.0)
        );
    }

    public Map<StatType, Double> asMap() {
        Map<StatType, Double> values = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            values.put(type, value(type));
        }
        return Map.copyOf(values);
    }

    private static void validateScalar(String label, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(label + " must be a finite, non-negative value");
        }
    }
}
