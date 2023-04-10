package com.protoevo.settings;

public class GraphicsSettings extends Settings {

    public final Settings.Parameter<Float> cameraZoomForCellDetails = new Settings.Parameter<>(
            "", "", .5f);
    public final Settings.Parameter<Integer> msaaSamples = new Settings.Parameter<>(
            "", "", 16);

    private GraphicsSettings() {
        super("Graphics");
    }

    public static GraphicsSettings createDefault() {
        GraphicsSettings settings = new GraphicsSettings();
        settings.collectParameters();
        return settings;
    }
}
