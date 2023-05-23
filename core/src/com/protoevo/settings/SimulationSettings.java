package com.protoevo.settings;

import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SimulationSettings extends Settings {

    public final Settings.Parameter<Long> simulationSeed = new Settings.Parameter<>(
            "Simulation Seed", "Random Seed", Utils::randomLong
    );
    public final Settings.Parameter<Float> simulationUpdateDelta = new Settings.Parameter<>(
            "Simulation Update Delta",
            "Amount of time to step the simulation with each update.",
            5f / 1000f,
            Statistics.ComplexUnit.TIME
    );
    public final Settings.Parameter<Boolean> finishOnProtozoaExtinction = new Settings.Parameter<>(
            "Finish on Extinction",
            "Whether to stop the simulation when all Protozoa go extinct.",
            true
    );
//    public final Settings.Parameter<Boolean> repopOnProtozoaExtinction = new Settings.Parameter<>(
//            "Repopulate on Extinction",
//            "Whether to repopulate the world when all Protozoa go extinct (overrides Finish on Extinction).",
//            true
//    );
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
    public final Settings.Parameter<Float> minParticleRadius = new Settings.Parameter<>(
            "Minimum Particle Radius",
            "Minimum radius of a particle.",
            3f / 100f);
    public final Settings.Parameter<Float> maxParticleRadius = new Settings.Parameter<>(
            "Maximum Particle Radius",
            "Maximum radius of a particle.",
            15f / 100f);
    public final Settings.Parameter<Float> maxMoleculeProductionRate = new Settings.Parameter<>(
            "Max Molecule Production Rate",
            "The maximum rate at which a cell can produce complex molecules.",
            .01f);
    public final Settings.Parameter<Float> moleculeProductionEnergyCost = new Settings.Parameter<>(
            "Molecule Production Energy Cost",
            "The energy cost for producing molecule.",
            1e4f);
    public Settings.Parameter<Integer> possibleMolecules = new Settings.Parameter<>(
            "Number of Possible Molecules",
            "The number of possible molecules that can be produced in the simulation.",
            128
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
