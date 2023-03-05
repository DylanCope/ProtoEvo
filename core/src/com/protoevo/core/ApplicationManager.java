package com.protoevo.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.utils.DebugMode;

import java.util.Map;

import static com.protoevo.utils.Utils.parseArgs;

public class ApplicationManager {

    private static boolean windowed = true;
    private volatile boolean headless = false, applicationRunning = true, saveOnExit = true;
    private Simulation simulation;
    private GraphicsAdapter graphics;

    public static void main(String[] args) {
        System.out.println("Current JVM version: " + System.getProperty("java.version"));
        Map<String, String> argsMap = parseArgs(args);

        ApplicationManager app = new ApplicationManager();

        if (argsMap.containsKey("headless") && Boolean.parseBoolean(argsMap.get("headless"))) {
            app.switchToHeadlessMode();
            app.setSimulation(new Simulation());
        }

        app.launch();
    }

    public void launch() {
        applicationRunning = true;

        while (applicationRunning) {
            if (!headless)
                createGraphics();
            else {
                loopUpdate();
            }
        }

        if (saveOnExit)
            saveAndCloseCurrentSimulation();

        System.exit(0);
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
        simulation.setManager(this);

        if (!simulation.isReady()) {
            if (!headless) {
                new Thread(simulation::prepare).start();
            } else {
                simulation.prepare();
            }
        } else {
            notifySimulationReady();
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
        if (simulation.isFinished()) {
            System.out.println("Simulation finished.");
            exit();
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
        saveOnExit = true;
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
        saveOnExit = false;

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
                GraphicsAdapter.settings.msaaSamples.get()); // 8, 8, 8, 8, 16, 0 are default values

        config.useVsync(true);
        config.setTitle("ProtoEvo");
        graphics = new GraphicsAdapter(this);
        // Creates graphics and runs updates from rendering loop
        new Lwjgl3Application(graphics, config);
    }
}
