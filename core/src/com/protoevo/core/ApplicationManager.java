package com.protoevo.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.protoevo.settings.RenderSettings;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.utils.DebugMode;

public class ApplicationManager {

    private static boolean windowed = false;
    private volatile boolean headless = false, applicationRunning = true;
    private Simulation simulation;
    private GraphicsAdapter graphics;

    public void launch() {
        createSimulation();

        applicationRunning = true;

        while (applicationRunning) {
            if (!headless)
                createGraphics();
            else {
                loopUpdate();
            }
        }
        System.out.println("ApplicationManager: Exiting application.");

        saveAndCloseCurrentSimulation();

        System.exit(0);
    }

    public void createSimulation() {
//        simulation = new Simulation(0, "chaos-nidoran-nobis");
        simulation = new Simulation();
        simulation.setManager(this);

        if (!headless) {
            new Thread(simulation::prepare).start();
        } else {
            simulation.prepare();
        }
    }

    public boolean hasSimulation() {
        return simulation != null;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void notifySimulationReady() {
        if (isGraphicsActive())
            graphics.notifySimulationReady();
    }

    public void loopUpdate() {
        headless = true;
        while (headless) {
            update();
        }
    }

    public void update() {
        if (hasSimulation() && simulation.isReady()) {
            simulation.update();
        }
    }

    public void saveAndCloseCurrentSimulation() {
        if (hasSimulation()) {
            simulation.close();
        }
    }

    public void exit() {
        closeGraphics();
        applicationRunning = false;
        headless = false;
    }

    public void closeGraphics() {
        if (graphics != null) {
            Gdx.app.exit();
            graphics = null;
        }
    }

    public void switchToHeadlessMode() {
        closeGraphics();
        headless = true;
    }

    public boolean isGraphicsActive() {
        return graphics != null;
    }

    public void toggleGraphics() {
        if (isGraphicsActive())
            switchToHeadlessMode();
        else
            headless = false;
    }

    public void createGraphics() {
        headless = false;

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
                getInputArguments().toString().contains("-agentlib:jdwp");

        if (isDebug)
            DebugMode.setMode(DebugMode.SIMPLE_INFO);

        if (isDebug | windowed) {
            config.setWindowedMode(1920, 1080);
        } else {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        }

        config.setBackBufferConfig(
                8, 8, 8, 8, 16, 0,
                RenderSettings.msaaSamples); // 8, 8, 8, 8, 16, 0 are default values

        config.useVsync(true);
        config.setTitle("ProtoEvo");
        graphics = new GraphicsAdapter(this);
        // Creates graphics and runs updates from rendering loop
        new Lwjgl3Application(graphics, config);
    }
}
