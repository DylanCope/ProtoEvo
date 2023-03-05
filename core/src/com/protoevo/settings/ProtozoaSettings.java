package com.protoevo.settings;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;

public class ProtozoaSettings extends Settings {
    public final Settings.Parameter<Float> geneExpressionInterval = new Settings.Parameter<>(
            "Simulation Update Delta",
            "Amount of time to step the simulation with each update.",
            Environment.settings.simulationUpdateDelta.get() * 10f,
            Statistics.ComplexUnit.TIME
    );
    public final Settings.Parameter<Boolean> matingEnabled = new Settings.Parameter<>(
            "",
            "",
            false);
    public final Settings.Parameter<Float> minBirthRadius = new Settings.Parameter<>(
            "",
            "",
            3f / 100f);
    public final Settings.Parameter<Float> maxProtozoanBirthRadius = new Settings.Parameter<>(
            "",
            "",
            8f / 100f);
    public final Settings.Parameter<Float> starvationFactor = new Settings.Parameter<>(
            "",
            "",
            .8f);
    public final Settings.Parameter<Float> initialGenomeConnectivity = new Settings.Parameter<>(
            "",
            "",
            0.5f);
    public final Settings.Parameter<Float> minHealthToSplit = new Settings.Parameter<>(
            "",
            "",
            0.15f);
    public final Settings.Parameter<Float> engulfForce = new Settings.Parameter<>(
            "",
            "",
            500f);
    public final Settings.Parameter<Float> engulfEatingRateMultiplier = new Settings.Parameter<>(
            "",
            "",
            3f);
    public final Settings.Parameter<Float> maxProtozoanSplitRadius = new Settings.Parameter<>(
            "",
            "",
            maxProtozoanBirthRadius.get() * 3f);
    public final Settings.Parameter<Float> minProtozoanSplitRadius = new Settings.Parameter<>(
            "",
            "",
            maxProtozoanBirthRadius.get() * 1.2f);
    public final Settings.Parameter<Float> minProtozoanGrowthRate = new Settings.Parameter<>(
            "",
            "",
            0f);
    public final Settings.Parameter<Float> maxProtozoanGrowthRate = new Settings.Parameter<>(
            "",
            "",
            1.5f);
    public final Settings.Parameter<Float> spikeDamage = new Settings.Parameter<>(
            "",
            "",
            3f);
    public final Settings.Parameter<Float> matingTime = new Settings.Parameter<>(
            "",
            "",
            0.1f);
    public final Settings.Parameter<Float> maxLightRange = new Settings.Parameter<>(
            "",
            "",
            Environment.settings.maxParticleRadius.get() * 5f);
    public final Settings.Parameter<Float> eatingConversionRatio = new Settings.Parameter<>(
            "",
            "",
            0.75f);
    public final Settings.Parameter<Float> maxFlagellumThrust = new Settings.Parameter<>(
            "",
            "",
            .005f);
    public final Settings.Parameter<Float> maxCiliaThrust = new Settings.Parameter<>(
            "",
            "",
            maxFlagellumThrust.get() / 10f);
    public final Settings.Parameter<Float> maxFlagellumTorque = new Settings.Parameter<>(
            "",
            "",
            .01f);
    public final Settings.Parameter<Float> maxCiliaTurn = new Settings.Parameter<>(
            "",
            "Radians per second",
            MathUtils.PI
    );

    public ProtozoaSettings() {
        super("Protozoa");
    }
}
