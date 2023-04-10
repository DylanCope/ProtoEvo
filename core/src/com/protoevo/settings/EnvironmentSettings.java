package com.protoevo.settings;

import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;

import java.util.Arrays;
import java.util.Collection;

public class EnvironmentSettings extends Settings {

    public final Settings.Parameter<Long> simulationSeed = new Settings.Parameter<>(
            "Simulation Seed", "Random Seed", 1L, false
    );
    public final Settings.Parameter<Float> simulationUpdateDelta = new Settings.Parameter<>(
            "Simulation Update Delta",
            "Amount of time to step the simulation with each update.",
            1f / 1000f,
            Statistics.ComplexUnit.TIME
    );
    public final Settings.Parameter<Boolean> finishOnProtozoaExtinction = new Settings.Parameter<>(
            "Finish on Extinction",
            "Whether to stop the simulation when all Protozoa go extinct.",
            true
    );
    public final Settings.Parameter<Boolean> repopOnProtozoaExtinction = new Settings.Parameter<>(
            "Repopulate on Extinction",
            "Whether to repopulate the world when all Protozoa go extinct (overrides Finish on Extinction).",
            true
    );
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
    public final Settings.Parameter<Float> cellRepairRate = new Settings.Parameter<>(
            "Cell Repair Rate",
            "",
            5e-1f
    );
    public final Settings.Parameter<Float> cellRepairMassFactor = new Settings.Parameter<>(
            "Cell Repair Mass Factor",
            "Amount of mass required to repair as a percent of the cell's mass.",
            0.05f
    );
    public final Settings.Parameter<Float> cellRepairEnergyFactor = new Settings.Parameter<>(
            "Cell Repair Energy Factor",
            "Amount of energy per unit mass required for repair.",
            5e2f
    );
    public final Settings.Parameter<Float> cellBindingResourceTransport = new Settings.Parameter<>(
            "Binding Resource Transport",
            "Percentage of a cell's resources transported in one unit of time",
            2.0f
    );
    public final Settings.Parameter<Float> plantEnergyDensity = new Settings.Parameter<>(
            "Plant Energy Density",
            "Energy per unit mass of plant material.",
            1e5f
    );
    public final Settings.Parameter<Float> meatEnergyDensity = new Settings.Parameter<>(
            "Meat Energy Density",
            "Energy per unit mass of meat material.",
            3e5f
    );

    public final Settings.Parameter<Float> meatDeathFactor = new Settings.Parameter<>(
            "Meat Death Factor",
            "How quickly meat cells die.",
            50f
    );
    public final Settings.Parameter<Boolean> enableChemicalField = new Settings.Parameter<>(
            "Chemical Field Enabled",
            "Whether or not to enable the chemical field.",
            true, false
    );
    public final Settings.Parameter<Float> minHealthToSplit = new Settings.Parameter<>(
            "Minimum Health to Split",
            "Amount of health needed to split and produce children.",
            0.5f
    );
    public final Settings.Parameter<Float> voidDamagePerSecond = new Settings.Parameter<>(
            "Void Damage Per Second",
            "",
            1f);
    public final Settings.Parameter<Float> minParticleRadius = new Settings.Parameter<>(
            "Minimum Particle Radius",
            "Minimum radius of a particle.",
            3f / 100f);
    public final Settings.Parameter<Float> maxParticleRadius = new Settings.Parameter<>(
            "Maximum Particle Radius",
            "Maximum radius of a particle.",
            15f / 100f);
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
    public final Settings.Parameter<Float> globalMutationChance = new Settings.Parameter<>(
            "Global Mutation Chance",
            "The fallback mutation chance for all mutations when there is no local choice.",
            0.05f);
    public final Settings.Parameter<Integer> initialGRNMutations = new Settings.Parameter<>(
            "Initial GRN Mutations",
            "The number of mutations to apply to the initial gene regulatory network (GRN).",
            3);
    public final Settings.Parameter<Float> minMutationChance = new Settings.Parameter<>(
            "Minimum Mutation Chance",
            "The minimum mutation chance for all mutations.",
            0.001f);
    public final Settings.Parameter<Float> maxMutationChance = new Settings.Parameter<>(
            "Maximum Mutation Chance",
            "Maximum mutation chance for all mutations.",
            0.1f);
    public final Settings.Parameter<Float> minTraitMutationChance = new Settings.Parameter<>(
            "Min Trait Mutation Chance",
            "",
            0.001f);
    public final Settings.Parameter<Float> maxTraitMutationChance = new Settings.Parameter<>(
            "Max Trait Mutation Chance",
            "",
            0.1f);
    public final Settings.Parameter<Float> minRegulationMutationChance = new Settings.Parameter<>(
            "Min Regulation Mutation Chance",
            "",
            0.001f);
    public final Settings.Parameter<Float> maxRegulationMutationChance = new Settings.Parameter<>(
            "Max Regulation Mutation Chance",
            "",
            0.05f);
    public final Settings.Parameter<Float> cellGrowthFactor = new Settings.Parameter<>(
            "Cell Growth Factor",
            "Controls how quickly cells can grow.",
            2e-2f);
    public final Settings.Parameter<Float> digestionFactor = new Settings.Parameter<>(
            "Digestion Factor",
            "Controls how quickly cells can digest food.",
            20f);
    public final Settings.Parameter<Float> chemicalDiffusionInterval = new Settings.Parameter<>(
            "Chemical Diffusion Interval",
            "How often to diffuse chemicals.",
            simulationUpdateDelta.get() * 20f);
    public final Settings.Parameter<Integer> chemicalFieldResolution = new Settings.Parameter<>(
            "Chemical Field Resolution",
            "How many cells wide the chemical field is.",
            1024);
    public final Settings.Parameter<Integer> lightMapResolution = new Settings.Parameter<>(
            "Light map Resolution",
            "How many cells wide the light map is.",
            256);
    public final Settings.Parameter<Float> maxLightEnvTemp = new Settings.Parameter<>(
            "Environment Light Temperature",
            "Environment temperature at in regions of maximum light.",
            15f);
    public final Settings.Parameter<Float> basicParticleMassDensity = new Settings.Parameter<>(
            "Base Particle Mass Density",
            "The mass density of a basic particle.",
            1f);
    public final Settings.Parameter<Float> maxMoleculeProductionRate = new Settings.Parameter<>(
            "Max Molecule Production Rate",
            "The maximum rate at which a cell can produce complex molecules.",
            .01f);
    public final Settings.Parameter<Double> deleteSynapseMutationRate = new Settings.Parameter<>(
            "Delete Synapse Mutation Rate",
            "The chance that a synapse will be deleted when mutating a cell.",
            0.1);
    public final Settings.Parameter<Double> deleteNeuronMutationRate = new Settings.Parameter<>(
            "Delete Neuron Mutation Rate",
            "The chance that a neuron will be deleted when mutating a cell.",
            0.1);
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
    public final Settings.Parameter<Float> fluidDragDampening = new Settings.Parameter<>(
            "Fluid Drag Dampening",
            "Controls the viscosity of the fluid.",
            10f);
    public Settings.Parameter<Integer> possibleMolecules = new Settings.Parameter<>(
            "Number of Possible Molecules",
            "The number of possible molecules that can be produced in the simulation.",
            128
    );
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
    public final Settings.Parameter<Float> plantDecayRate = new Settings.Parameter<>(
            "Plant Decay Rate",
            "The rate at which plant matter decays.",
            1e-4f
    );
    public final Settings.Parameter<Float> meatDecayRate = new Settings.Parameter<>(
            "Meat Decay Rate",
            "The rate at which meat matter decays.",
            1e-3f
    );


    public WorldGenerationSettings world;
    public PlantSettings plant;
    public ProtozoaSettings protozoa;
    public MiscSettings misc;

    private EnvironmentSettings() {
        super("Environment");
    }

    public static EnvironmentSettings createDefault() {
        EnvironmentSettings settings = new EnvironmentSettings();
        settings.collectParameter();
        Environment.settings = settings;

        settings.world = new WorldGenerationSettings();
        settings.world.collectParameter();

        settings.plant = new PlantSettings();
        settings.plant.collectParameter();

        settings.protozoa = new ProtozoaSettings();
        settings.protozoa.collectParameter();

        settings.misc = new MiscSettings();
        settings.misc.collectParameter();

        return settings;
    }

    public Collection<Settings> getSettings() {
        return Arrays.asList(this, world, plant, protozoa, misc);
    }

    public Settings getSettings(String basicName) {
        switch (basicName) {
            case "world":
                return world;
            case "protozoa":
                return protozoa;
            case "plant":
                return plant;
            case "misc":
                return misc;
            default:
                return this;
        }
    }
}
