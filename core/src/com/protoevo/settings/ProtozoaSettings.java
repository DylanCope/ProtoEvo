package com.protoevo.settings;

public class ProtozoaSettings {
    public static final float minProtozoanBirthRadius = 3f / 100f;
    public static final float maxProtozoanBirthRadius = 8f / 100f;
    public static final float protozoaStarvationFactor = 0.05f;
    public static final float retinaCellGrowthCost = .03f;
    public static final float minHealthToSplit = 0.5f;
    public static final float engulfForce = 500f;
    public static final float maxProtozoanSplitRadius = maxProtozoanBirthRadius * 3f;
    public static final float minProtozoanSplitRadius = maxProtozoanBirthRadius * 1.5f;
    public static final float minProtozoanGrowthRate = 5e-6f;
    public static final float maxProtozoanGrowthRate = 5e-5f;
    public static final float spikeGrowthPenalty = .08f;
    public static final float spikeMovementPenalty = 0.97f;
    public static final float spikePlantConsumptionPenalty = 0.8f;
    public static final float spikeDeathRatePenalty = 0.01f;
    public static final float maxSpikeGrowth = 0.1f;
    public static final float spikeDamage = 3f;
    public static final float matingTime = 0.1f;
    public static final float protozoaLightRange = SimulationSettings.maxParticleRadius * 5f;
    public static final float eatingConversionRatio = 0.75f;
    public static final float maxProtozoaThrust = .005f;
    public static final float maxProtozoaTorque = .01f;
}
