package com.protoevo.settings;

import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.utils.Geometry;

public class PlantSettings extends Settings {
    
    public final Settings.Parameter<Float> minBirthRadius = new Settings.Parameter<>(
            "Min Birth Radius",
            "The minimum radius of a plant at birth.",
            2f / 100f);
    public final Settings.Parameter<Float> maxBirthRadius = new Settings.Parameter<>(
            "Max Birth Radius",
            "The maximum radius of a plant at birth.",
            8f / 100f);
    public final Settings.Parameter<Float> minPlantGrowth = new Settings.Parameter<>(
            "Min Plant Growth",
            "The minimum growth factor a plant can have.",
            0f);
    public final Settings.Parameter<Float> maxPlantGrowth = new Settings.Parameter<>(
            "Max Plant Growth",
            "The maximum growth factor a plant can have.",
            1f);
    public final Settings.Parameter<Float> collisionDestructionRate = new Settings.Parameter<>(
            "Contact Death Rate",
            "The rate at which a plant's health is reduced when it collides with another non-plant object.",
            Geometry.getCircleArea(maxBirthRadius.get())
                    * Environment.settings.basicParticleMassDensity.get()
    );
    public final Settings.Parameter<Float> minHealthToSplit = new Settings.Parameter<>(
            "Min Health to Split",
            "Minimum health required to produce children.",
            0.15f);
    public final Settings.Parameter<Float> geneExpressionInterval = new Settings.Parameter<>(
            "Simulation Update Delta",
            "Amount of time to step the simulation with each update.",
            Environment.settings.simulationUpdateDelta.get() * 100f,
            Statistics.ComplexUnit.TIME
    );

    public PlantSettings() {
        super("Plant");
    }
}
