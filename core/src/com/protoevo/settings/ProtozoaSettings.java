package com.protoevo.settings;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;

public class ProtozoaSettings extends Settings {
    public final Parameter<Float> geneExpressionInterval = new Parameter<>(
            "Gene Expression Interval",
            "The amount of in-simulation time between ticking the Gene Regulatory Networks of protozoa.",
            Environment.settings.simulationUpdateDelta.get() * 10f,
            Statistics.ComplexUnit.TIME
    );
    public final Parameter<Boolean> matingEnabled = new Parameter<>(
            "Mating Enabled",
            "Is mating enabled?",
            true);
    public final Parameter<Float> minBirthRadius = new Parameter<>(
            "Min Birth Radius",
            "The minimum radius of a protozoan at birth.",
            3f / 100f);
    public final Parameter<Float> maxBirthRadius = new Parameter<>(
            "Max Birth Radius",
            "The maximum radius of a protozoan at birth.",
            8f / 100f);
    public final Parameter<Float> starvationFactor = new Parameter<>(
            "Starvation Factor",
            "The rate at which a protozoan's health is reduced when it is not eating.",
            .85f);
    public final Parameter<Float> minHealthToSplit = new Parameter<>(
            "Min Health to Split",
            "The minimum health required to produce children.",
            0.15f);
    public final Parameter<Float> engulfForce = new Parameter<>(
            "Engulf Force",
            "The force applied to a particle when engulfed.",
            500f);
    public final Parameter<Float> engulfEatingRateMultiplier = new Parameter<>(
            "Engulf Eating Rate Multiplier",
            "The speed at which a protozoan eats and engulfed cell.",
            1.75f);
    public final Parameter<Float> engulfRangeFactor = new Parameter<>(
            "Engulf Range Factor",
            "The fraction of the cell radius away that a victim cell needs to be from the " +
                    "phagocytosis node in order to be engulfed. When this value is set to 1 it means that " +
                    "a minimum of 3 nodes are be required to cover the entire circumference " +
                    "of the cell.",
            1.5f);
//    public final Settings.Parameter<Float> maxProtozoanSplitRadius = new Settings.Parameter<>(
//            "Max Split Radius",
//            "The maximum radius of a protozoan after splitting.",
//            maxBirthRadius.get() * 3f);
//    public final Settings.Parameter<Float> minProtozoanSplitRadius = new Settings.Parameter<>(
//            "",
//            "",
//            maxBirthRadius.get() * 1.2f);
    public final Parameter<Float> minProtozoanGrowthRate = new Parameter<>(
            "Min Growth Rate",
            "The minimum growth factor of a protozoan.",
            0f);
    public final Parameter<Float> maxProtozoanGrowthRate = new Parameter<>(
            "Max Growth Rate",
            "The maximum growth factor of a protozoan.",
            1.5f);
    public final Parameter<Float> spikeDamage = new Parameter<>(
            "Spike Damage",
            "The amount of damage a spike does to a protozoan.",
            5f);
//    public final Settings.Parameter<Float> matingTime = new Settings.Parameter<>(
//            "",
//            "",
//            0.1f);
    public final Parameter<Float> maxLightRange = new Parameter<>(
            "Light Range",
            "The maximum range of light.",
            Environment.settings.maxParticleRadius.get() * 10f);
//    public final Settings.Parameter<Float> eatingConversionRatio = new Settings.Parameter<>(
//            "Eating Conversion Ratio",
//            "",
//            0.75f);
    public final Parameter<Float> maxFlagellumThrust = new Parameter<>(
            "Max Flagellum Thrust",
            "The maximum thrust of a flagellum.",
            .006f);
    public final Parameter<Float> maxCiliaThrust = new Parameter<>(
            "Max Cilia Thrust",
            "The maximum thrust of a cilia.",
            .0005f);
    public final Parameter<Float> maxFlagellumTorque = new Parameter<>(
            "Max Flagellum Torque",
            "The maximum torque of a flagellum.",
            .005f);
    public final Parameter<Float> maxCiliaTurn = new Parameter<>(
            "Max Cilia Turn",
            "Maximum turn produced by a cilia in radians per second",
            MathUtils.PI * .75f
    );
    public final Parameter<Boolean> separatePhagoNodes = new Parameter<>(
            "Separate Phagocytosis Nodes",
            "Different nodes for engulfing meat and plants?",
            true);

    public ProtozoaSettings() {
        super("Protozoa");
    }
}
