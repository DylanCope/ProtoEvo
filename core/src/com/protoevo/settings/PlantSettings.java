package com.protoevo.settings;

import com.protoevo.env.Environment;
import com.protoevo.utils.Geometry;

public class PlantSettings extends Settings {
    
    public final Settings.Parameter<Float> minBirthRadius = new Settings.Parameter<>(
            "",
            "",
            2f / 100f);
    public final Settings.Parameter<Float> maxBirthRadius = new Settings.Parameter<>(
            "",
            "",
            8f / 100f);
    public final Settings.Parameter<Float> minPlantGrowth = new Settings.Parameter<>(
            "",
            "",
            0f);
    public final Settings.Parameter<Float> maxPlantGrowth = new Settings.Parameter<>(
            "",
            "",
            1f);
    public final Settings.Parameter<Float> collisionDestructionRate = new Settings.Parameter<>(
            "",
            "",
            Geometry.getCircleArea(maxBirthRadius.get())
                    * Environment.settings.basicParticleMassDensity.get()
    );
    public final Settings.Parameter<Float> minHealthToSplit = new Settings.Parameter<>(
            "Min Health to Split",
            "Minimum health required to produce children.",
            0.15f);

    public PlantSettings() {
        super("Plant");
    }
}
