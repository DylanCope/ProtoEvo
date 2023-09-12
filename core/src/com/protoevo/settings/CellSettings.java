package com.protoevo.settings;

import com.protoevo.networking.Server;

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
            1e-3f);
    public final Settings.Parameter<Float> chemicalExtractionMeatConversion = new Settings.Parameter<>(
            "Chemical Extraction Meat Conversion",
            "The amount of food extracted from meat matter in the chemical solution.",
            1e-4f);
    public final Settings.Parameter<Float> chemicalExtractionFactor = new Settings.Parameter<>(
            "Chemical Extraction Factor",
            "The amount to dilute the chemical solution by when extracting food.",
            100f);
    public final Settings.Parameter<Double> energyRequiredForGrowth = new Settings.Parameter<>(
            "Energy Required For Growth",
            "Factor controlling how much energy is required for growth.",
            1e3);
    public final Settings.Parameter<Float> growthFactor = new Settings.Parameter<>(
            "Cell Growth Factor",
            "Controls how quickly cells can grow.",
            3e-2f);
    public final Settings.Parameter<Float> digestionFactor = new Settings.Parameter<>(
            "Digestion Factor",
            "Controls how quickly cells can digest food.",
            20f);
    public final Settings.Parameter<Float> temperatureDeathRate = new Settings.Parameter<>(
            "Temperature Death Rate",
            "Rate at which a cell looses health when outside its temperature tolerance range.",
            .01f);
    public final Settings.Parameter<Float> minTemperatureTolerance = new Settings.Parameter<>(
            "Min Temperature Tolerance",
            "Minimum temperature tolerance (+/- degrees before suffering adverse effects).",
            2f);
    public final Settings.Parameter<Float> maxTemperatureTolerance = new Settings.Parameter<>(
            "Max Temperature Tolerance",
            "Maximum temperature tolerance (+/- degrees before suffering adverse effects).",
            5f);
    public final Settings.Parameter<Float> temperatureToleranceEnergyCost = new Settings.Parameter<>(
            "Temperature Tolerance Energy Cost",
            "Energy cost per degree of temperature tolerance per unit time.",
            1 / 100f);
    public final Settings.Parameter<Float> activityHeatGeneration = new Settings.Parameter<>(
            "Cell Temperature Death Rate",
            "Heat generated per unit activity per unit time.",
            3f);
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
    public final Settings.Parameter<Float> repairActivity = new Settings.Parameter<>(
            "Cell Repair Activity",
            "Amount of activity generated by repairing.",
            1f
    );
    public final Settings.Parameter<Float> growthActivity = new Settings.Parameter<>(
            "Cell Growth Activity",
            "Amount of activity generated by growing.",
            1f
    );
    public final Settings.Parameter<Float> digestionActivity = new Settings.Parameter<>(
            "Cell Digestion Activity",
            "Amount of activity generated per mass generated.",
            1f
    );
    public final Settings.Parameter<Float> kineticEnergyActivity = new Settings.Parameter<>(
            "Kinetic Energy Activity",
            "Amount of activity generated per kinetic energy generated.",
            .1f
    );
    public final Settings.Parameter<Float> grnHiddenNodeActivity = new Settings.Parameter<>(
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
