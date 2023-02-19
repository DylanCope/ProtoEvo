package com.protoevo.settings;

public class SimulationSettings {
    public static final long simulationSeed = 1;
    public static final float simulationUpdateDelta = 1f / 1000f;
    public static final int physicsPositionIterations = 1;
    public static final int physicsVelocityIterations = 1;
    public static final float voidStartDistance = 3 * WorldGenerationSettings.environmentRadius;
    public static final float voidStartDistance2 = voidStartDistance * voidStartDistance;
    public static final float voidDamagePerSecond = 1f;
    public static final float spatialHashRadius = voidStartDistance;

    public static final int maxPlants = 10000;
    public static final int maxProtozoa = 5000;
    public static final int maxMeat = 1000;

    public static final float minParticleRadius = 3f / 100f;
    public static final float maxParticleRadius = 15f / 100f;
    public static final float startingAvailableCellEnergy = 1f;
    public static final float startingAvailableConstructionMass = 10e-3f;
    public static final float globalMutationChance = 0.05f;
    public static final float minMutationChance = 0.001f;
    public static final float maxMutationChance = 0.1f;

    public static final float cellGrowthFactor = 1e-2f;
    public static final float digestionFactor = 2f;
    public static final float chemicalDiffusionInterval = simulationUpdateDelta * 20f;
    public static final int chemicalFieldResolution = 1024;
    public static final float chemicalFieldRadius = voidStartDistance;
    public static int spatialHashResolution = 20;
    public static final boolean simulationOnSeparateThread = false;
    public static float basicParticleMassDensity = 1f;
    public static float maxMoleculeProductionRate = .01f;
    public static double deleteSynapseMutationRate = 0.1;
    public static double deleteNeuronMutationRate = 0.1;
}
