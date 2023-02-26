package com.protoevo.settings;

import com.badlogic.gdx.math.MathUtils;

public class ProtozoaSettings {
    public static final float minProtozoanBirthRadius = 3f / 100f;
    public static final float maxProtozoanBirthRadius = 8f / 100f;
    public static final float protozoaStarvationFactor = .8f;
    public static final float minHealthToSplit = 0.15f;
    public static final float engulfForce = 500f;
    public static final float maxProtozoanSplitRadius = maxProtozoanBirthRadius * 3f;
    public static final float minProtozoanSplitRadius = maxProtozoanBirthRadius * 1.2f;
    public static final float minProtozoanGrowthRate = 0;
    public static final float maxProtozoanGrowthRate = 1.5f;
    public static final float spikeDamage = 3f;
    public static final float matingTime = 0.1f;
    public static final float protozoaLightRange = SimulationSettings.maxParticleRadius * 5f;
    public static final float eatingConversionRatio = 0.75f;
    public static final float maxFlagellumThrust = .005f;
    public static final float maxCiliaThrust = maxFlagellumThrust / 10f;
    public static final float maxFlagellumTorque = .01f;
    public static float maxCiliaTurn = MathUtils.PI; // radians per second
}
