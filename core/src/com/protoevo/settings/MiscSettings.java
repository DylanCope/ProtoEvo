package com.protoevo.settings;

import com.protoevo.core.Statistics;

public class MiscSettings extends Settings {

    public final Settings.Parameter<Float> timeBetweenSaves = new Settings.Parameter<>(
            "Time Between Saves",
            "Amount of in-simulation time to wait until making a new save.",
            50.0f,
            Statistics.ComplexUnit.TIME
    );
    public final Settings.Parameter<Float> historySnapshotTime = new Settings.Parameter<>(
            "Save Statistics Time",
            "Amount of in-simulation time between making a new snapshot of the summary statistics.",
            20.0f, Statistics.ComplexUnit.TIME
    );
    public final Settings.Parameter<Boolean> writeGenomes = new Settings.Parameter<>(
            "Write Genomes",
            "Whether or not to write genome information (warning: generates lots of data).",
            false
    );
    public final Settings.Parameter<Integer> physicsPositionIterations = new Settings.Parameter<>(
            "Physics Position Iterations",
            "",
            1);
    public final Settings.Parameter<Integer> physicsVelocityIterations = new Settings.Parameter<>(
            "Physics Velocity Iterations",
            "",
            1);
    public final Settings.Parameter<Integer> maxPlants = new Settings.Parameter<>(
            "",
            "",
            4000);
    public final Settings.Parameter<Integer> maxProtozoa = new Settings.Parameter<>(
            "",
            "",
            2000);
    public final Settings.Parameter<Integer> maxMeat = new Settings.Parameter<>(
            "",
            "",
            2000);
    public final Settings.Parameter<Integer> protozoaLocalCap = new Settings.Parameter<>(
            "",
            "",
            300);
    public final Settings.Parameter<Integer> plantLocalCap = new Settings.Parameter<>(
            "",
            "",
            75);
    public final Settings.Parameter<Integer> meatLocalCap = new Settings.Parameter<>(
            "",
            "",
            75);
    public final Settings.Parameter<Boolean> useGPU = new Settings.Parameter<>(
            "",
            "",
            true);
    public final Settings.Parameter<Integer> spatialHashResolution = new Settings.Parameter<>(
            "",
            "",
            15);

    public MiscSettings() {
        super("Misc");
    }
}
