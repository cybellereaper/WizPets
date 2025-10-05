package com.github.cybellereaper.wizpets.api;

import java.util.Objects;
import java.util.Random;

/**
 * Immutable set of pet battle statistics.
 */
public final class StatSet {
    private final double health;
    private final double attack;
    private final double defense;
    private final double magic;

    public StatSet(double health, double attack, double defense, double magic) {
        this.health = health;
        this.attack = attack;
        this.defense = defense;
        this.magic = magic;
    }

    public double getHealth() {
        return health;
    }

    public double getAttack() {
        return attack;
    }

    public double getDefense() {
        return defense;
    }

    public double getMagic() {
        return magic;
    }

    public double value(StatType type) {
        return switch (type) {
            case HEALTH -> health;
            case ATTACK -> attack;
            case DEFENSE -> defense;
            case MAGIC -> magic;
        };
    }

    public StatSet update(StatType type, double value) {
        return switch (type) {
            case HEALTH -> new StatSet(value, attack, defense, magic);
            case ATTACK -> new StatSet(health, value, defense, magic);
            case DEFENSE -> new StatSet(health, attack, value, magic);
            case MAGIC -> new StatSet(health, attack, defense, value);
        };
    }

    public StatSet breedWith(StatSet other, Random random) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(random, "random");
        return new StatSet(
            mix(health, other.health, random),
            mix(attack, other.attack, random),
            mix(defense, other.defense, random),
            mix(magic, other.magic, random)
        );
    }

    private static double mix(double a, double b, Random random) {
        double base = (a + b) / 2.0;
        double variation = random.nextDouble(-2.75, 2.75);
        return Math.max(1.0, base + variation);
    }

    public static StatSet randomEV(Random random) {
        Objects.requireNonNull(random, "random");
        return new StatSet(
            random.nextDouble(0.0, 64.0),
            random.nextDouble(0.0, 64.0),
            random.nextDouble(0.0, 64.0),
            random.nextDouble(0.0, 64.0)
        );
    }

    public static StatSet randomIV(Random random) {
        Objects.requireNonNull(random, "random");
        return new StatSet(
            random.nextDouble(0.0, 15.0),
            random.nextDouble(0.0, 15.0),
            random.nextDouble(0.0, 15.0),
            random.nextDouble(0.0, 15.0)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatSet other)) {
            return false;
        }
        return Double.compare(other.health, health) == 0
            && Double.compare(other.attack, attack) == 0
            && Double.compare(other.defense, defense) == 0
            && Double.compare(other.magic, magic) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(health, attack, defense, magic);
    }

    @Override
    public String toString() {
        return "StatSet{"
            + "health=" + health
            + ", attack=" + attack
            + ", defense=" + defense
            + ", magic=" + magic
            + '}';
    }
}
