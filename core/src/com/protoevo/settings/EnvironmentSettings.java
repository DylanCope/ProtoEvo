package com.protoevo.settings;

public class EnvironmentSettings extends Settings {
    public final Settings.Parameter<Float> chemicalDiffusionInterval = new Settings.Parameter<>(
            "Chemical Diffusion Interval",
            "How often to diffuse chemicals.",
            20f);
    public final Settings.Parameter<Float> maxLightEnvTemp = new Settings.Parameter<>(
            "Environment Light Temperature",
            "Environment temperature at in regions of maximum light.",
            15f);
    public final Settings.Parameter<Float> fluidDragDampening = new Settings.Parameter<>(
            "Fluid Drag Dampening",
            "Controls the viscosity of the fluid.",
            10f);
    public final Settings.Parameter<Float> voidDamagePerSecond = new Settings.Parameter<>(
            "Void Damage Per Second",
            "",
            1f);
    public final Settings.Parameter<Float> dayNightCycleLength = new Settings.Parameter<>(
            "Day/Night Cycle Length",
            "The amount of time to cycle a day.",
            250f);
    public final Settings.Parameter<Float> nightPercentage = new Settings.Parameter<>(
            "Night Time Percentage",
            "Percentage of day/night cycle to spend in night.",
            0.15f);
    public final Settings.Parameter<Float> nightLightLevel = new Settings.Parameter<>(
            "Night Light Level",
            "The light level at night.",
            0.15f);
    public final Settings.Parameter<Float> dayNightTransition = new Settings.Parameter<>(
            "Day/Night Transition",
            "Percentage of day/night cycle spent transitioning between day and night.",
            0.05f);

    public EnvironmentSettings() {
        super("Environment");
    }
}
