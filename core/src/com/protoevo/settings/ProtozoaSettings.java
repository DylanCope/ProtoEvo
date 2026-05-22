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
    // Senescence: age-driven damage that fires REGARDLESS of energy state.
    // Without this, a well-fed cell that hit max radius would never die — no
    // energy starvation, growth stopped, splits blocked by health <
    // minHealthToSplit, just sits there indefinitely. Players reported cells
    // alive for 2000+ sim-seconds doing nothing.
    //
    // Curve: below maxLifespan -> no senescence damage. Past maxLifespan,
    // damage rate grows quadratically with how far over the cell is:
    //     excess = (timeAlive - maxLifespan) / maxLifespan   // in lifespans
    //     rate   = excess² × senescenceDeathRate              // health/sec
    //
    // At 1× lifespan: 0 damage. At 2× lifespan: peak rate. At 3× lifespan:
    // 4× peak rate (guaranteed death within tens of sim-seconds). So a healthy
    // protozoan typically dies somewhere between 1.5× and 2× maxLifespan,
    // never significantly past 3×. Tunable per-run via the settings UI.
    public final Parameter<Float> maxLifespan = new Parameter<>(
            "Max Lifespan",
            "Sim-seconds of life before age-driven damage starts accumulating.",
            // 500 → 1500. With 500, cells reach effective death at ~750-1000
            // sim-sec — but the ratchet interval is now 240s and the
            // reproductive cycle takes 60-120s, so a cell at the old cap
            // only saw 3-5 reproductive opportunities and 2-4 ratchet
            // steps before dying. Not enough generations for selection to
            // track each tightening step. 1500s lifespan (effective death
            // ~2250-3000s) gives lineages a real shot at evolving through
            // each ratchet step before the next one lands.
            1500f);
    public final Parameter<Float> senescenceDeathRate = new Parameter<>(
            "Senescence Death Rate",
            "Peak senescence damage in health/sec (reached at 2× max lifespan, "
            + "scales quadratically past max lifespan).",
            0.02f);
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
    // Bumped from 0 → 0.4. The evolvable "Growth Rate" trait gets remapped
    // from [0,1] to [minGrowthRate, maxGrowthRate]; with a floor of 0, a
    // lineage that evolved its trait toward 0 had effective growth rate ~0
    // and never reached split radius, no matter how well it ate. Worse,
    // construction mass that arrived from feeding got immediately consumed
    // by the cell's surface nodes (each of which tries to build its
    // attachment every tick), so the user saw "mass appears then vanishes
    // with no growth." A non-zero floor guarantees growth wins some of the
    // mass each tick.
    public final Parameter<Float> minProtozoanGrowthRate = new Parameter<>(
            "Min Growth Rate",
            "The minimum growth factor of a protozoan.",
            0.4f);
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
