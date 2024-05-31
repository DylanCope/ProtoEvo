package com.protoevo.settings;

public class CellSettings extends Settings {
    public Parameter<Float> complexMoleculeDecayRate = new Parameter<>(
            "Complex Molecule Decay Rate",
            "The rate at which complex molecules decay.",
            1e-6f
    );
    public Parameter<Float> energyDecayRate = new Parameter<>(
            "Energy Decay Rate",
            "The rate at which energy storage decays.",
            .05f
    );
    public final Parameter<Float> chemicalExtractionPlantConversion = new Parameter<>(
            "Chemical Extraction Plant Conversion",
            "The amount of food extracted from plant matter in the chemical solution.",
            1e-3f);
    public final Parameter<Float> chemicalExtractionMeatConversion = new Parameter<>(
            "Chemical Extraction Meat Conversion",
            "The amount of food extracted from meat matter in the chemical solution.",
            1e-4f);
    public final Parameter<Float> chemicalExtractionFactor = new Parameter<>(
            "Chemical Extraction Factor",
            "The amount to dilute the chemical solution by when extracting food.",
            100f);
    public final Parameter<Double> energyRequiredForGrowth = new Parameter<>(
            "Energy Required For Growth",
            "Factor controlling how much energy is required for growth.",
            1e3);
    public final Parameter<Float> growthFactor = new Parameter<>(
            "Cell Growth Factor",
            "Controls how quickly cells can grow.",
            3e-2f);
    public final Parameter<Float> digestionFactor = new Parameter<>(
            "Digestion Factor",
            "Controls how quickly cells can digest food.",
            20f);
    public final Parameter<Float> temperatureDeathRate = new Parameter<>(
            "Temperature Death Rate",
            "Rate at which a cell looses health when outside its temperature tolerance range.",
            .01f);
    public final Parameter<Float> minTemperatureTolerance = new Parameter<>(
            "Min Temperature Tolerance",
            "Minimum temperature tolerance (+/- degrees before suffering adverse effects).",
            2f);
    public final Parameter<Float> maxTemperatureTolerance = new Parameter<>(
            "Max Temperature Tolerance",
            "Maximum temperature tolerance (+/- degrees before suffering adverse effects).",
            5f);
    public final Parameter<Float> temperatureToleranceEnergyCost = new Parameter<>(
            "Temperature Tolerance Energy Cost",
            "Energy cost per degree of temperature tolerance per unit time.",
            1 / 100f);
    public final Parameter<Float> activityHeatGeneration = new Parameter<>(
            "Cell Temperature Death Rate",
            "Heat generated per unit activity per unit time.",
            3f);
    public final Parameter<Float> basicParticleMassDensity = new Parameter<>(
            "Base Particle Mass Density",
            "The mass density of a basic particle.",
            1f);
    public final Parameter<Float> startingAvailableCellEnergy = new Parameter<>(
            "Starting Available Cell Energy",
            "Starting amount of energy available to cells.",
            1f);
    public final Parameter<Float> energyCapFactor = new Parameter<>(
            "Energy Cap Factor",
            "Maximum energy a cell can have at the minimum size.",
            500f);
    public final Parameter<Float> startingAvailableConstructionMass = new Parameter<>(
            "Starting Available Construction Mass",
            "Starting amount of construction mass available to cells.",
            10e-3f);
    public final Parameter<Float> engulfExtractionWasteMultiplier = new Parameter<>(
            "Engulf Extraction Waste Multiplier",
            "Multiplier of mass removed from engulfed cell for each unit extracted.",
            1.05f
    );
    //    public final Settings.Parameter<Float> engulfRangeFactor = new Settings.Parameter<>(
//            "Engulf Range Factor",
//            "Multiplier of mass removed from engulfed cell for each unit extracted.",
//            1.05f
//    );
    public final Parameter<Float> repairRate = new Parameter<>(
            "Cell Repair Rate",
            "",
            5e-1f
    );
    public final Parameter<Float> repairMassFactor = new Parameter<>(
            "Cell Repair Mass Factor",
            "Amount of mass required to repair as a percent of the cell's mass.",
            0.05f
    );
    public final Parameter<Float> repairEnergyFactor = new Parameter<>(
            "Cell Repair Energy Factor",
            "Amount of energy per unit mass required for repair.",
            5e2f
    );
    public final Parameter<Float> bindingResourceTransport = new Parameter<>(
            "Binding Resource Transport",
            "Percentage of a cell's resources transported in one unit of time",
            2.0f
    );
    public final Parameter<Float> repairActivity = new Parameter<>(
            "Cell Repair Activity",
            "Amount of activity generated by repairing.",
            1f
    );
    public final Parameter<Float> growthActivity = new Parameter<>(
            "Cell Growth Activity",
            "Amount of activity generated by growing.",
            1f
    );
    public final Parameter<Float> digestionActivity = new Parameter<>(
            "Cell Digestion Activity",
            "Amount of activity generated per mass generated.",
            1f
    );
    public final Parameter<Float> kineticEnergyActivity = new Parameter<>(
            "Kinetic Energy Activity",
            "Amount of activity generated per kinetic energy generated.",
            .1f
    );
    public final Parameter<Float> grnHiddenNodeActivity = new Parameter<>(
            "GRN Node Activity",
            "Amount of cell activity generated per GRN node activation.",
            .1f
    );
//    public Settings.Parameter<Integer> surfaceNodeDim = new Settings.Parameter<>(
//            "Surface Node Dimension",
//            "Dimension of the surface node I/O channel. Should be 1 or 3.",
//            1,
//            false
//    );

    public CellSettings() {
        super("Cell");
    }
}
