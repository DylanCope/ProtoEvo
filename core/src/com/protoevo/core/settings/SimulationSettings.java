package com.protoevo.core.settings;

public class SimulationSettings {
    public static final long simulationSeed = 1;
    public static final float simulationUpdateDelta = 1f / 1000f;
    public static final int physicsSubSteps = 3;

    public static final int maxPlants = 7000;
    public static final int maxProtozoa = 1500;
    public static final int maxMeat = 1000;

    public static final float minParticleRadius = 3f / 100f;
    public static final float maxParticleRadius = 15f / 100f;
    public static final float startingAvailableCellEnergy = 0.01f;
}
