package com.protoevo.settings;

import com.protoevo.core.Statistics;

public class MiscSettings extends Settings {

    public final Parameter<Float> timeBetweenHistoricalSaves = new Parameter<>(
            "Time Between Saves",
            "Amount of in-simulation time to wait until making a new historical save.",
            500.0f,
            Statistics.ComplexUnit.TIME);
    public final Parameter<Float> timeBetweenAutoSaves = new Parameter<>(
            "Time Between Auto Saves",
            "Amount of in-simulation time to wait until making a new autosave.",
            50.0f,
            Statistics.ComplexUnit.TIME);
    public final Parameter<Integer> numberOfAutoSaves = new Parameter<>(
            "Number of Auto Saves",
            "Number of autosaves to keep.",
            3,
            Statistics.ComplexUnit.COUNT);
    public final Parameter<Float> statisticsSnapshotTime = new Parameter<>(
            "Save Statistics Time",
            "Amount of in-simulation time between making a new snapshot of the summary statistics.",
            20.0f, Statistics.ComplexUnit.TIME);
    public final Parameter<Boolean> writeGenomes = new Parameter<>(
            "Write Genomes",
            "Whether or not to write genome information (warning: generates lots of data).",
            false);
    public final Parameter<Integer> physicsPositionIterations = new Parameter<>(
            "Physics Position Iterations",
            "Number of iterations to run the physics engine's position solver.",
            1);
    public final Parameter<Integer> physicsVelocityIterations = new Parameter<>(
            "Physics Velocity Iterations",
            "Number of iterations to run the physics engine's velocity solver.",
            1);
    public final Parameter<Integer> maxPlants = new Parameter<>(
            "Max Plants",
            "The maximum number of plants that can exist in the simulation.",
            1500);
    public final Parameter<Integer> maxProtozoa = new Parameter<>(
            "Max Protozoa",
            "The maximum number of protozoa that can exist in the simulation.",
            500);
    public final Parameter<Integer> maxMeat = new Parameter<>(
            "Max Meat",
            "The maximum number of meat that can exist in the simulation.",
            750);
    public final Parameter<Integer> protozoaLocalCap = new Parameter<>(
            "Local Protozoa Cap",
            "The maximum number of protozoa that can exist in a local region (defined by the spatial hash resolution).",
            250);
    public final Parameter<Integer> plantLocalCap = new Parameter<>(
            "Local Plant Cap",
            "The maximum number of plants that can exist in a local region (defined by the spatial hash resolution).",
            100);
    public final Parameter<Integer> meatLocalCap = new Parameter<>(
            "Local Meat Cap",
            "The maximum number of meat that can exist in a local region (defined by the spatial hash resolution).",
            75);
    public final Parameter<Boolean> useCUDA = new Parameter<>(
            "Use CUDA",
            "Whether or not to use the CUDA for accelerating calculations on the GPU.",
            false);
    public final Parameter<Boolean> useOpenGLComputeShader = new Parameter<>(
            "Use OpenGL Compute Shader",
            "Whether or not to use OpenGL compute shaders for accelerating calculations on the GPU.",
            true);
    public final Parameter<Integer> chemicalCPUIterations = new Parameter<>(
            "CPU Chemical Diffusion Iterations",
            "Number of chemical diffusion iterations to perform when running on the CPU.",
            100000);
    public final Parameter<Integer> spatialHashResolution = new Parameter<>(
            "Spatial Hash Resolution",
            "The resolution of the spatial hash used for local population caps.",
            20);
    public final Parameter<Float> checkCellJoiningsInterval = new Parameter<>(
            "Check Cell Joinings Interval",
            "The amount of time between checking for cell joinings.",
            0.1f
    );


    public MiscSettings() {
        super("Misc");
    }
}
