package com.github.cybellereaper.wizpets.api;

/**
 * Enumeration of supported pet statistics with human readable labels.
 */
public enum StatType {
    HEALTH("Health"),
    ATTACK("Attack"),
    DEFENSE("Defense"),
    MAGIC("Magic");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
