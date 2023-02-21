package com.protoevo.settings;

import com.protoevo.utils.Geometry;

public class PlantSettings {
    public static final float minMaxPlantRadius = 0.015f;
    public static final float minPlantSplitRadius = 0.01f;
    public static final float minPlantBirthRadius = 2f / 100f;
    public static final float maxPlantBirthRadius = 8f / 100f;

    public static final float minPlantGrowth = 0;
    public static final float maxPlantGrowth = 1f;
    public static final float plantCrowdingGrowthDecay = 1.0f;
    public static final float plantCriticalCrowding = 6.0f;
    public static final float plantRegen = .2f;
    public static float collisionDestructionRate =
            5f * Geometry.getCircleArea(maxPlantBirthRadius) * SimulationSettings.basicParticleMassDensity;
}