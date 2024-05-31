package com.protoevo.settings;

import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.utils.Utils;

import java.util.Arrays;
import java.util.List;

public class SimulationSettings extends Settings {

    public final Parameter<Long> simulationSeed = new Parameter<>(
            "Simulation Seed", "Random Seed", Utils::randomLong
    );
    public final Parameter<Float> simulationUpdateDelta = new Parameter<>(
            "Simulation Update Delta",
            "Amount of time to step the simulation with each update.",
            1f / 1000f,
            Statistics.ComplexUnit.TIME
    );
    public final Parameter<Boolean> finishOnProtozoaExtinction = new Parameter<>(
            "Finish on Extinction",
            "Whether to stop the simulation when all Protozoa go extinct.",
            true
    );
//    public final Settings.Parameter<Boolean> repopOnProtozoaExtinction = new Settings.Parameter<>(
//            "Repopulate on Extinction",
//            "Whether to repopulate the world when all Protozoa go extinct (overrides Finish on Extinction).",
//            true
//    );
    public final Parameter<Float> plantEnergyDensity = new Parameter<>(
            "Plant Energy Density",
            "Energy per unit mass of plant material.",
            1e5f
    );
    public final Parameter<Float> meatEnergyDensity = new Parameter<>(
            "Meat Energy Density",
            "Energy per unit mass of meat material.",
            3e5f
    );

    public final Parameter<Float> meatDeathFactor = new Parameter<>(
            "Meat Death Factor",
            "How quickly meat cells die.",
            50f
    );
    public final Parameter<Boolean> enableChemicalField = new Parameter<>(
            "Chemical Field Enabled",
            "Whether or not to enable the chemical field.",
            true, false
    );
    public final Parameter<Float> minHealthToSplit = new Parameter<>(
            "Minimum Health to Split",
            "Amount of health needed to split and produce children.",
            0.5f
    );
    public final Parameter<Float> minParticleRadius = new Parameter<>(
            "Minimum Particle Radius",
            "Minimum radius of a particle.",
            2f / 100f);
    public final Parameter<Float> maxParticleRadius = new Parameter<>(
            "Maximum Particle Radius",
            "Maximum radius of a particle.",
            15f / 100f);
    public final Parameter<Float> maxMoleculeProductionRate = new Parameter<>(
            "Max Molecule Production Rate",
            "The maximum rate at which a cell can produce complex molecules.",
            .01f);
    public final Parameter<Float> moleculeProductionEnergyCost = new Parameter<>(
            "Molecule Production Energy Cost",
            "The energy cost for producing molecule.",
            1e4f);
    public Parameter<Integer> possibleMolecules = new Parameter<>(
            "Number of Possible Molecules",
            "The number of possible molecules that can be produced in the simulation.",
            32
    );
    public final Parameter<Float> plantDecayRate = new Parameter<>(
            "Plant Decay Rate",
            "The rate at which plant matter decays.",
            1e-4f
    );
    public final Parameter<Float> meatDecayRate = new Parameter<>(
            "Meat Decay Rate",
            "The rate at which meat matter decays.",
            1e-3f
    );


    public WorldGenerationSettings worldgen;
    public PlantSettings plant;
    public ProtozoaSettings protozoa;
    public MiscSettings misc;
    public CellSettings cell;
    public EvolutionSettings evo;
    public EnvironmentSettings env;

    private SimulationSettings() {
        super("Simulation");
    }

    public static SimulationSettings createDefault() {
        SimulationSettings settings = new SimulationSettings();
        settings.collectParameters();
        Environment.settings = settings;

        settings.env = new EnvironmentSettings();
        settings.env.collectParameters();

        settings.cell = new CellSettings();
        settings.cell.collectParameters();

        settings.worldgen = new WorldGenerationSettings();
        settings.worldgen.collectParameters();

        settings.plant = new PlantSettings();
        settings.plant.collectParameters();

        settings.protozoa = new ProtozoaSettings();
        settings.protozoa.collectParameters();

        settings.misc = new MiscSettings();
        settings.misc.collectParameters();

        settings.evo = new EvolutionSettings();
        settings.evo.collectParameters();

        return settings;
    }

    public List<Settings> getSettings() {
        return Arrays.asList(this, env, cell, worldgen, plant, protozoa, evo, misc);
    }

    public Settings getSettings(String basicName) {
        switch (basicName) {
            case "world":
                return worldgen;
            case "protozoa":
                return protozoa;
            case "plant":
                return plant;
            case "misc":
                return misc;
            case "cell":
                return cell;
            case "env":
                return env;
            case "evo":
                return evo;
            default:
                return this;
        }
    }
}
