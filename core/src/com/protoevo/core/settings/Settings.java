package com.protoevo.core.settings;

public final class Settings {
    // Simulation settings
    public static final long simulationSeed = 1;
    public static final float simulationUpdateDelta = 1f / 1000f;
    public static final float maxProtozoaSpeed = .01f;
    public static final float maxParticleSpeed = 1e-4f;
    public static final float timeBetweenSaves = 2000.0f;
    public static final float historySnapshotTime = 2.0f;
    public static final boolean writeGenomes = false;
    public static final boolean finishOnProtozoaExtinction = true;
    public static final int numPossibleCAMs = 64;
    public static final float camProductionEnergyCost = 0.05f;
    public static final float foodExtractionWasteMultiplier = 1.5f;
    public static final float cellRepairRate = 0.5f;
    public static final float occludingBindingEnergyTransport = 0.5f;
    public static final float channelBindingEnergyTransport = 0.5f;
    public static final boolean enableAnchoringBinding = false;
    public static final boolean enableOccludingBinding = false;
    public static final boolean enableChannelFormingBinding = true;
    public static final boolean enableSignalRelayBinding = false;

    public static final float plantEnergyRate = 1f;
    public static final float plantConstructionRate = 10f;
    public static final float plantEnergyDensity = 1f;
    public static final float meatEnergyDensity = 10f;

    // Tank settings

    // Chemical settings
    public static final boolean enableChemicalField = true;
//    public static final int numChemicalBreaks = numChunkBreaks * 4;
    public static final float chemicalUpdateTime = simulationUpdateDelta * 10f;
    public static final float chemicalDiffusionRate = 1f;
    public static final float pheromoneFlow = 0.05f;
    public static final float plantPheromoneDeposit = 500f;

    // Protozoa settings
    public static final float minProtozoanBirthRadius = 3f / 100f;
    public static final float maxProtozoanBirthRadius = 8f / 100f;
    public static final float protozoaStarvationFactor = 0.006f;
    public static final int defaultRetinaSize = 0;
    public static final int maxRetinaSize = 8;
    public static final float retinaCellGrowthCost = .03f;
    public static final int numContactSensors = 0;
    public static final float minRetinaRayAngle = (float) Math.toRadians(10);
    public static final float minHealthToSplit = 0.5f;
    public static final float maxProtozoanSplitRadius = 0.03f;
    public static final float minProtozoanSplitRadius = 0.015f;
    public static final float minProtozoanGrowthRate = .05f;
    public static final float maxProtozoanGrowthRate = .1f;
    public static final int maxTurnAngle = 25;
    public static final float spikeGrowthPenalty = .08f;
    public static final float spikeMovementPenalty = 0.97f;
    public static final float spikePlantConsumptionPenalty = 0.8f;
    public static final float spikeDeathRatePenalty = 0.01f;
    public static final float maxSpikeGrowth = 0.1f;
    public static final float spikeDamage = 3f;
    public static final float matingTime = 0.1f;
    public static final float globalMutationChance = 20 * 0.05f;
    public static final float minMutationChance = 0.001f;
    public static final float maxMutationChance = 0.1f;
    public static final float protozoaInteractRange = 8 * 20f;
    public static final float eatingConversionRatio = 0.75f;

    // Plant Settings
    public static final float minMaxPlantRadius = 0.015f;
    public static final float minPlantSplitRadius = 0.01f;
    public static final float minPlantBirthRadius = 2f / 100f;
    public static final float maxPlantBirthRadius = 8f / 100f;

    public static final float minPlantGrowth = 0.02f;
    public static final float plantGrowthRange = 0.05f;
    public static final float plantCrowdingGrowthDecay = 1.0f;
    public static final float plantCriticalCrowding = 6.0f;
    public static final float plantRegen = .2f;

    public static final float protozoaMinMaxTurn = 0.0174533f; // (float) Math.toRadians(1);
    public static final float protozoaMaxMaxTurn = 0.10472f; // (float) Math.toRadians(6);

    // Stats

    public static final float statsDistanceScalar = 1.0f;
    public static final float statsTimeScalar = 100.0f;
    public static final float statsMassScalar = 1f;

    // Rendering

    public static final boolean showFPS = false;
    public static final boolean antiAliasing = true;
}
