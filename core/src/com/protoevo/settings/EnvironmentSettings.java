package com.protoevo.settings;

public class EnvironmentSettings extends Settings {
    public final Parameter<Float> chemicalDiffusionInterval = new Parameter<>(
            "Chemical Diffusion Interval",
            "How often to diffuse chemicals.",
            20f);
    public final Parameter<Float> maxLightEnvTemp = new Parameter<>(
            "Environment Light Temperature",
            "Environment temperature at in regions of maximum light.",
            15f);
    public final Parameter<Float> fluidDragDampening = new Parameter<>(
            "Fluid Drag Dampening",
            "Controls the viscosity of the fluid.",
            10f);
    public final Parameter<Float> voidDamagePerSecond = new Parameter<>(
            "Void Damage Per Second",
            "Factor controlling how much damage being outside the environment does.",
            10f);

    public final Parameter<Boolean> dayNightCycleEnabled = new Parameter<>(
            "Day/Night Cycle Enabled",
            "Whether the light level varies cyclically.",
            false);
    public final Parameter<Float> dayNightCycleLength = new Parameter<>(
            "Day/Night Cycle Length",
            "The amount of time to cycle a day.",
            250f);
    public final Parameter<Float> nightPercentage = new Parameter<>(
            "Night Time Percentage",
            "Percentage of day/night cycle to spend in night.",
            0.1f);
    public final Parameter<Float> nightLightLevel = new Parameter<>(
            "Night Light Level",
            "The light level at night.",
            0.55f);
    public final Parameter<Float> dayNightTransition = new Parameter<>(
            "Day/Night Transition",
            "Percentage of day/night cycle spent transitioning between day and night.",
            0.05f);

    public EnvironmentSettings() {
        super("Environment");
    }
}
