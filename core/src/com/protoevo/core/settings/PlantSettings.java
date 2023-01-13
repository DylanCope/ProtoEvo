package com.protoevo.core.settings;

public class PlantSettings {
    public static final float minMaxPlantRadius = 0.015f;
    public static final float minPlantSplitRadius = 0.01f;
    public static final float minPlantBirthRadius = 2f / 100f;
    public static final float maxPlantBirthRadius = 8f / 100f;

    public static final float minPlantGrowth = 0.01f;
    public static final float maxPlantGrowth = 0.04f;
    public static final float plantCrowdingGrowthDecay = 1.0f;
    public static final float plantCriticalCrowding = 6.0f;
    public static final float plantRegen = .2f;

    public static final float protozoaMinMaxTurn = 0.0174533f; // (float) Math.toRadians(1);
    public static final float protozoaMaxMaxTurn = 0.10472f; // (float) Math.toRadians(6);
}
