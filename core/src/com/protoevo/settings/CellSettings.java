package com.protoevo.settings;

public class CellSettings extends Settings {
    public Settings.Parameter<Float> complexMoleculeDecayRate = new Settings.Parameter<>(
            "Complex Molecule Decay Rate",
            "The rate at which complex molecules decay.",
            1e-6f
    );
    public Settings.Parameter<Float> energyDecayRate = new Settings.Parameter<>(
            "Energy Decay Rate",
            "The rate at which energy storage decays.",
            .05f
    );
    public final Settings.Parameter<Float> chemicalExtractionPlantConversion = new Settings.Parameter<>(
            "Chemical Extraction Plant Conversion",
            "The amount of food extracted from plant matter in the chemical solution.",
            5e-4f);
    public final Settings.Parameter<Float> chemicalExtractionMeatConversion = new Settings.Parameter<>(
            "Chemical Extraction Meat Conversion",
            "The amount of food extracted from meat matter in the chemical solution.",
            5e-6f);
    public final Settings.Parameter<Float> chemicalExtractionFactor = new Settings.Parameter<>(
            "Chemical Extraction Factor",
            "The amount to dilute the chemical solution by when extracting food.",
            100f);
    public final Settings.Parameter<Double> energyRequiredForGrowth = new Settings.Parameter<>(
            "Energy Required For Growth",
            "Factor controlling how much energy is required for growth.",
            1e4);
    public final Settings.Parameter<Float> growthFactor = new Settings.Parameter<>(
            "Cell Growth Factor",
            "Controls how quickly cells can grow.",
            2e-2f);
    public final Settings.Parameter<Float> digestionFactor = new Settings.Parameter<>(
            "Digestion Factor",
            "Controls how quickly cells can digest food.",
            20f);
    public final Settings.Parameter<Float> cellTemperatureDeathRate = new Settings.Parameter<>(
            "Cell Temperature Death Rate",
            "Rate at which a cell looses health when outside its temperature tolerance range.",
            2f);
    public final Settings.Parameter<Float> activityHeatGeneration = new Settings.Parameter<>(
            "Cell Temperature Death Rate",
            "Heat generated per unit activity per unit time.",
            2f);
    public final Settings.Parameter<Float> basicParticleMassDensity = new Settings.Parameter<>(
            "Base Particle Mass Density",
            "The mass density of a basic particle.",
            1f);
    public final Settings.Parameter<Float> startingAvailableCellEnergy = new Settings.Parameter<>(
            "Starting Available Cell Energy",
            "Starting amount of energy available to cells.",
            1f);
    public final Settings.Parameter<Float> energyCapFactor = new Settings.Parameter<>(
            "Energy Cap Factor",
            "Maximum energy a cell can have at the minimum size.",
            500f);
    public final Settings.Parameter<Float> startingAvailableConstructionMass = new Settings.Parameter<>(
            "Starting Available Construction Mass",
            "Starting amount of construction mass available to cells.",
            10e-3f);
    public final Settings.Parameter<Float> engulfExtractionWasteMultiplier = new Settings.Parameter<>(
            "Engulf Extraction Waste Multiplier",
            "Multiplier of mass removed from engulfed cell for each unit extracted.",
            1.05f
    );
    //    public final Settings.Parameter<Float> engulfRangeFactor = new Settings.Parameter<>(
//            "Engulf Range Factor",
//            "Multiplier of mass removed from engulfed cell for each unit extracted.",
//            1.05f
//    );
    public final Settings.Parameter<Float> repairRate = new Settings.Parameter<>(
            "Cell Repair Rate",
            "",
            5e-1f
    );
    public final Settings.Parameter<Float> repairMassFactor = new Settings.Parameter<>(
            "Cell Repair Mass Factor",
            "Amount of mass required to repair as a percent of the cell's mass.",
            0.05f
    );
    public final Settings.Parameter<Float> repairEnergyFactor = new Settings.Parameter<>(
            "Cell Repair Energy Factor",
            "Amount of energy per unit mass required for repair.",
            5e2f
    );
    public final Settings.Parameter<Float> bindingResourceTransport = new Settings.Parameter<>(
            "Binding Resource Transport",
            "Percentage of a cell's resources transported in one unit of time",
            2.0f
    );

    public CellSettings() {
        super("Cell");
    }
}
