package com.protoevo.settings.legacy;

public class LegacySimulationSettings {
    public static final long simulationSeed = 1;
    public static final float simulationUpdateDelta = 1f / 1000f;
    public static final int physicsPositionIterations = 1;
    public static final int physicsVelocityIterations = 1;
    public static final float voidStartDistance = 1.25f * LegacyWorldGenerationSettings.environmentRadius;
    public static final float voidStartDistance2 = voidStartDistance * voidStartDistance;
    public static final float voidDamagePerSecond = 1f;
    public static final float spatialHashRadius = voidStartDistance;

    public static final int maxPlants = 4000;
    public static final int maxProtozoa = 2000;
    public static final int maxMeat = 2000;
    public static final int protozoaLocalCap = 300;
    public static final int plantLocalCap = 75;
    public static final int meatLocalCap = 75;

    public static final float minParticleRadius = 3f / 100f;
    public static final float maxParticleRadius = 15f / 100f;
    public static final float startingAvailableCellEnergy = 1f;
    public static final float startingAvailableConstructionMass = 10e-3f;
    public static final float globalMutationChance = 0.05f;
    public static final float initialGRNMutations = 3;
    public static final float minMutationChance = 0.001f;
    public static final float maxMutationChance = 0.1f;
    public static final float minTraitMutationChance = 0.001f;
    public static final float maxTraitMutationChance = 0.1f;
    public static final float minRegulationMutationChance = 0.001f;
    public static final float maxRegulationMutationChance = 0.05f;

    public static final float cellGrowthFactor = 2e-2f;
    public static final float digestionFactor = 20f;
    public static final float chemicalDiffusionInterval = simulationUpdateDelta * 20f;
    public static final int chemicalFieldResolution = 1024;
    public static final float chemicalFieldRadius = voidStartDistance;
    public static int spatialHashResolution = 15;
    public static final boolean simulationOnSeparateThread = false;
    public static float basicParticleMassDensity = 1f;
    public static float maxMoleculeProductionRate = .01f;
    public static double deleteSynapseMutationRate = 0.1;
    public static double deleteNeuronMutationRate = 0.1;
    public static float chemicalExtractionFoodConversion = 3e-5f;
    public static float chemicalExtractionFactor = 100f;  // colour removed per unit time
    public static double energyRequiredForGrowth = 1e4f;
    public static float fluidDragDampening = 10f;
    public static boolean matingEnabled = false;
}
