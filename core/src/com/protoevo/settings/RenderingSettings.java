package com.protoevo.settings;

public class RenderingSettings extends Settings {

    public final Parameter<Float> cameraZoomForCellDetails = new Parameter<>(
            "", "", .5f);
    public final Parameter<Float> damageVisualLingerTime = new Parameter<>(
            "", "", .5f);
    public final Parameter<Float> damageVisualRedAmount = new Parameter<>(
            "", "", .5f);
    public final Parameter<Float> maxDPSAmountForRedness = new Parameter<>(
            "", "", .25f);
    public final Parameter<Float> minDPSAmountForRedness = new Parameter<>(
            "", "", .1f);

    private RenderingSettings() {
        super("Rendering");
    }

    public static RenderingSettings createDefault() {
        RenderingSettings settings = new RenderingSettings();
        settings.collectParameters();
        return settings;
    }
}
