package com.protoevo.core.settings;

public class WorldGenerationSettings {
    public static final int worldGenerationSeed = 1;
    public static final int numInitialProtozoa = 500;
    public static final int numInitialPlantPellets = 5000;
    public static final boolean initialPopulationClustering = true;
    public static final int numRingClusters = 6;
    public static final int numPopulationClusters = 3;
    public static final float populationClusterRadius = 30f;
    public static final float rockClusterRadius = 2f;
    public static final float populationClusterRadiusRange = 0.f;
    public static final float environmentRadius = 10.0f;
    public static final boolean sphericalTank = false;
    public static final int numChunkBreaks = 100;
    public static final float maxParticleRadius = 15f / 100f;
    public static final float tankFluidResistance = 8e-4f;
    public static final float brownianFactor = 1000f;
    public static final float coefRestitution = 0.005f;
    public static final float maxRockSize = .6f; // 0.15f;
    public static final float minRockSize = .2f; //0.05f;
    public static final float minRockSpikiness = (float) Math.toRadians(45);
    public static final float minRockOpeningSize = 200f / 1000f;
    public static final int rockGenerationIterations = 0;
    public static final int rockSeedingIterations = 0;
    public static final float rockClustering = 0.95f;
    public static final float ringBreakProbability = 0.05f;
    public static final float ringBreakAngleMinSkip = (float) Math.toRadians(5);
    public static final float ringBreakAngleMaxSkip = (float) Math.toRadians(30);
}
