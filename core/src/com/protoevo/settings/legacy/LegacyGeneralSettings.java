package com.protoevo.settings.legacy;

public final class LegacyGeneralSettings {
    // Simulation settings
    public static final long simulationSeed = 1;
    public static final float simulationUpdateDelta = 1f / 1000f;
    public static final float maxProtozoaSpeed = .01f;
    public static final float maxParticleSpeed = 1e-4f;
    public static final float timeBetweenSaves = 50.0f;
    public static final float historySnapshotTime = 20.0f;
    public static final boolean writeGenomes = false;
    public static final boolean finishOnProtozoaExtinction = true;
    public static final int numPossibleCAMs = 64;
    public static final float camProductionEnergyCost = 0.05f;
    public static final float engulfExtractionWasteMultiplier = 1.05f;
    public static final float cellRepairRate = 5e-1f;
    public static final float cellRepairMassFactor = 0.05f; // % of your mass is required to repair
    public static final float cellRepairEnergyFactor = 5e2f; // % of your mass is required to repair
    public static final float cellBindingResourceTransport = 0.5f; // % of the resource is transported in 1s
    public static final boolean enableAnchoringBinding = false;
    public static final boolean enableOccludingBinding = false;
    public static final boolean enableChannelFormingBinding = true;
    public static final boolean enableSignalRelayBinding = false;

    public static final float plantPhotosynthesizeEnergyRate = 300f;
    public static final float plantConstructionRate = 10f;
    public static final float plantEnergyDensity = 1e5f;
    public static final float meatEnergyDensity = 2e6f;

    // Tank settings

    // Chemical settings
    public static final boolean enableChemicalField = true;
    // Protozoa settings
    public static final float minHealthToSplit = 0.5f;
}
