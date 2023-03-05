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
            "Mating Enabled",
            "Is mating enabled?",
            false);
    public final Settings.Parameter<Float> minBirthRadius = new Settings.Parameter<>(
            "Min Birth Radius",
            "The minimum radius of a protozoan at birth.",
            3f / 100f);
    public final Settings.Parameter<Float> maxBirthRadius = new Settings.Parameter<>(
            "Max Birth Radius",
            "The maximum radius of a protozoan at birth.",
            8f / 100f);
    public final Settings.Parameter<Float> starvationFactor = new Settings.Parameter<>(
            "Starvation Factor",
            "The rate at which a protozoan's health is reduced when it is not eating.",
            .8f);
    public final Settings.Parameter<Float> initialGenomeConnectivity = new Settings.Parameter<>(
            "Initial Genome Connectivity",
            "The initial connectivity of a protozoan's genome.",
            0.5f);
    public final Settings.Parameter<Float> minHealthToSplit = new Settings.Parameter<>(
            "Min Health to Split",
            "The minimum health required to produce children.",
            0.15f);
    public final Settings.Parameter<Float> engulfForce = new Settings.Parameter<>(
            "Engulf Force",
            "The force applied to a particle when engulfed.",
            500f);
    public final Settings.Parameter<Float> engulfEatingRateMultiplier = new Settings.Parameter<>(
            "Engulf Eating Rate Multiplier",
            "The speed at which a protozoan eats and engulfed cell.",
            3.5f);
//    public final Settings.Parameter<Float> maxProtozoanSplitRadius = new Settings.Parameter<>(
//            "Max Split Radius",
//            "The maximum radius of a protozoan after splitting.",
//            maxBirthRadius.get() * 3f);
//    public final Settings.Parameter<Float> minProtozoanSplitRadius = new Settings.Parameter<>(
//            "",
//            "",
//            maxBirthRadius.get() * 1.2f);
    public final Settings.Parameter<Float> minProtozoanGrowthRate = new Settings.Parameter<>(
            "Min Growth Rate",
            "The minimum growth factor of a protozoan.",
            0f);
    public final Settings.Parameter<Float> maxProtozoanGrowthRate = new Settings.Parameter<>(
            "Max Growth Rate",
            "The maximum growth factor of a protozoan.",
            1.5f);
    public final Settings.Parameter<Float> spikeDamage = new Settings.Parameter<>(
            "Spike Damage",
            "The amount of damage a spike does to a protozoan.",
            4f);
//    public final Settings.Parameter<Float> matingTime = new Settings.Parameter<>(
//            "",
//            "",
//            0.1f);
    public final Settings.Parameter<Float> maxLightRange = new Settings.Parameter<>(
            "Light Range",
            "The maximum range of light.",
            Environment.settings.maxParticleRadius.get() * 5f);
//    public final Settings.Parameter<Float> eatingConversionRatio = new Settings.Parameter<>(
//            "Eating Conversion Ratio",
//            "",
//            0.75f);
    public final Settings.Parameter<Float> maxFlagellumThrust = new Settings.Parameter<>(
            "Max Flagellum Thrust",
            "The maximum thrust of a flagellum.",
            .005f);
    public final Settings.Parameter<Float> maxCiliaThrust = new Settings.Parameter<>(
            "Max Cilia Thrust",
            "The maximum thrust of a cilia.",
            maxFlagellumThrust.get() / 10f);
    public final Settings.Parameter<Float> maxFlagellumTorque = new Settings.Parameter<>(
            "Max Flagellum Torque",
            "The maximum torque of a flagellum.",
            .01f);
    public final Settings.Parameter<Float> maxCiliaTurn = new Settings.Parameter<>(
            "Max Cilia Turn",
            "Maximum turn produced by a cilia in radians per second",
            MathUtils.PI
    );

    public ProtozoaSettings() {
        super("Protozoa");
    }
}
