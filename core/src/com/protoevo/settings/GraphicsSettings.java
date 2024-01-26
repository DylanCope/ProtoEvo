package com.protoevo.settings;

public class GraphicsSettings extends Settings {

    public final Settings.Parameter<Float> cameraZoomForCellDetails = new Settings.Parameter<>(
            "", "", .5f);
    public final Settings.Parameter<Integer> msaaSamples = new Settings.Parameter<>(
            "", "", 16);
    public final Settings.Parameter<Float> damageVisualLingerTime = new Settings.Parameter<>(
            "", "", .5f);
    public final Settings.Parameter<Float> damageVisualRedAmount = new Settings.Parameter<>(
            "", "", .5f);
    public final Settings.Parameter<Float> maxDPSAmountForRedness = new Settings.Parameter<>(
            "", "", .25f);
    public final Settings.Parameter<Float> minDPSAmountForRedness = new Settings.Parameter<>(
            "", "", .1f);
    private GraphicsSettings() {
        super("Graphics");
    }

    public static GraphicsSettings createDefault() {
        GraphicsSettings settings = new GraphicsSettings();
        settings.collectParameters();
        return settings;
    }
}
