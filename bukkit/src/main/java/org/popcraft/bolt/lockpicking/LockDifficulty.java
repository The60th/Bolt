package org.popcraft.bolt.lockpicking;

/**
 * Difficulty presets for both lockpicking minigames.
 */
public enum LockDifficulty {

    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    private final String displayName;

    LockDifficulty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // ========== Tension & Sweep Presets ==========

    /** Number of pins the player must set to win. */
    public int getTensionPins() {
        return switch (this) {
            case EASY -> 2;
            case MEDIUM -> 3;
            case HARD -> 4;
        };
    }

    /** Width of the sweet-spot zone (number of slots). */
    public int getTensionSweetSpotWidth() {
        return switch (this) {
            case EASY -> 3;
            case MEDIUM -> 2;
            case HARD -> 1;
        };
    }

    /** Ticks between each tension bar decay. */
    public int getTensionDecayInterval() {
        return switch (this) {
            case EASY -> 40;
            case MEDIUM -> 25;
            case HARD -> 15;
        };
    }

    /** Tension bars lost on a wrong set attempt. */
    public int getTensionWrongPenalty() {
        return switch (this) {
            case EASY -> 2;
            case MEDIUM -> 3;
            case HARD -> 4;
        };
    }

    /** Whether the sweet spot shifts periodically. */
    public boolean getTensionSweetSpotShifts() {
        return switch (this) {
            case EASY, MEDIUM -> false;
            case HARD -> true;
        };
    }

    /** Ticks between sweet-spot shifts (only used when shifts are enabled). */
    public int getTensionShiftInterval() {
        return 60;
    }

    // ========== Rake & Set Presets ==========

    /** Number of pins the player must set to win. */
    public int getRakePins() {
        return switch (this) {
            case EASY -> 5;
            case MEDIUM -> 7;
            case HARD -> 9;
        };
    }

    /** Number of picks the player starts with. */
    public int getRakeStartingPicks() {
        return switch (this) {
            case EASY -> 8;
            case MEDIUM -> 7;
            case HARD -> 6;
        };
    }

    /** Ticks a popped pin stays visible before retracting. */
    public int getRakePopWindow() {
        return switch (this) {
            case EASY -> 30;
            case MEDIUM -> 20;
            case HARD -> 12;
        };
    }

    /** Maximum number of pins that can be popped at the same time. */
    public int getRakeMaxSimultaneous() {
        return switch (this) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };
    }

    /** Chance (0.0-1.0) that a popped pin is a trap. */
    public double getRakeTrapChance() {
        return switch (this) {
            case EASY -> 0.0;
            case MEDIUM -> 0.10;
            case HARD -> 0.25;
        };
    }

    /** Chance (0.0-1.0) that a popped pin is jammed (requires 2 clicks). */
    public double getRakeJamChance() {
        return switch (this) {
            case EASY -> 0.0;
            case MEDIUM -> 0.15;
            case HARD -> 0.30;
        };
    }

    /** Consecutive successful sets needed to earn a bonus pick. */
    public int getRakeComboThreshold() {
        return 3;
    }

    /** Speed-up multiplier applied to pop interval as pins are set (0.0-1.0 range, lower = faster). */
    public double getRakeSpeedRamp() {
        return switch (this) {
            case EASY -> 0.85;
            case MEDIUM -> 0.75;
            case HARD -> 0.65;
        };
    }

    /** Ticks between each wave of pin pop-ups. */
    public int getRakePopInterval() {
        return switch (this) {
            case EASY -> 30;
            case MEDIUM -> 20;
            case HARD -> 12;
        };
    }

    /**
     * Parse from string, case-insensitive. Returns null if not recognized.
     */
    public static LockDifficulty fromString(String name) {
        if (name == null) return null;
        return switch (name.toLowerCase()) {
            case "easy" -> EASY;
            case "medium" -> MEDIUM;
            case "hard" -> HARD;
            default -> null;
        };
    }
}
