package com.protoevo.settings;

import com.protoevo.core.Statistics;

public class MiscSettings extends Settings {

    public final Settings.Parameter<Float> timeBetweenHistoricalSaves = new Settings.Parameter<>(
            "Time Between Saves",
            "Amount of in-simulation time to wait until making a new historical save.",
            500.0f,
            Statistics.ComplexUnit.TIME);
    public final Settings.Parameter<Float> timeBetweenAutoSaves = new Settings.Parameter<>(
            "Time Between Auto Saves",
            "Amount of in-simulation time to wait until making a new autosave.",
            50.0f,
            Statistics.ComplexUnit.TIME);
    public final Settings.Parameter<Integer> numberOfAutoSaves = new Settings.Parameter<>(
            "Number of Auto Saves",
            "Number of autosaves to keep.",
            3,
            Statistics.ComplexUnit.COUNT);
    public final Settings.Parameter<Float> statisticsSnapshotTime = new Settings.Parameter<>(
            "Save Statistics Time",
            "Amount of in-simulation time between making a new snapshot of the summary statistics.",
            20.0f, Statistics.ComplexUnit.TIME);
    public final Settings.Parameter<Boolean> writeGenomes = new Settings.Parameter<>(
            "Write Genomes",
            "Whether or not to write genome information (warning: generates lots of data).",
            false);
    public final Settings.Parameter<Integer> physicsPositionIterations = new Settings.Parameter<>(
            "Physics Position Iterations",
            "Number of iterations to run the physics engine's position solver.",
            1);
    public final Settings.Parameter<Integer> physicsVelocityIterations = new Settings.Parameter<>(
            "Physics Velocity Iterations",
            "Number of iterations to run the physics engine's velocity solver.",
            1);
    public final Settings.Parameter<Integer> maxPlants = new Settings.Parameter<>(
            "Max Plants",
            "The maximum number of plants that can exist in the simulation.",
            1500);
    public final Settings.Parameter<Integer> maxProtozoa = new Settings.Parameter<>(
            "Max Protozoa",
            "The maximum number of protozoa that can exist in the simulation.",
            500);
    public final Settings.Parameter<Integer> maxMeat = new Settings.Parameter<>(
            "Max Meat",
            "The maximum number of meat that can exist in the simulation.",
            750);
    public final Settings.Parameter<Integer> protozoaLocalCap = new Settings.Parameter<>(
            "Local Protozoa Cap",
            "The maximum number of protozoa that can exist in a local region (defined by the spatial hash resolution).",
            250);
    public final Settings.Parameter<Integer> plantLocalCap = new Settings.Parameter<>(
            "Local Plant Cap",
            "The maximum number of plants that can exist in a local region (defined by the spatial hash resolution).",
            100);
    public final Settings.Parameter<Integer> meatLocalCap = new Settings.Parameter<>(
            "Local Meat Cap",
            "The maximum number of meat that can exist in a local region (defined by the spatial hash resolution).",
            75);
    public final Settings.Parameter<Boolean> useCUDA = new Settings.Parameter<>(
            "Use CUDA",
            "Whether or not to use the CUDA for accelerating calculations on the GPU.",
            true);
    public final Settings.Parameter<Boolean> useOpenGL = new Settings.Parameter<>(
            "Use OpenGL",
            "Whether or not to use OpenGL compute shaders for accelerating calculations on the GPU.",
            true);
    public final Settings.Parameter<Integer> chemicalCPUIterations = new Settings.Parameter<>(
            "CPU Chemical Diffusion Iterations",
            "Number of chemical diffusion iterations to perform when running on the CPU.",
            100000);
    public final Settings.Parameter<Integer> spatialHashResolution = new Settings.Parameter<>(
            "Spatial Hash Resolution",
            "The resolution of the spatial hash used for local population caps.",
            20);
    public final Settings.Parameter<Float> checkCellJoiningsInterval = new Settings.Parameter<>(
            "Check Cell Joinings Interval",
            "The amount of time between checking for cell joinings.",
            0.1f
    );


    public MiscSettings() {
        super("Misc");
    }
}
