package com.protoevo.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.protoevo.networking.RemoteGraphics;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.utils.DebugMode;

import java.util.Map;

import static com.protoevo.utils.Utils.parseArgs;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;

public class ApplicationManager {

    public final static boolean windowed = false, borderlessWindowed = true;
    public static long window = 0;
    private volatile boolean headless = false, applicationRunning = true, saveOnExit = true;
    private boolean onlyHeadless = false;
    private Simulation simulation;
    private GraphicsAdapter graphics;
    private RemoteGraphics remoteGraphics;
    private volatile boolean sendRemoteGraphicsRequested = false;

    public static void main(String[] args) {
        System.out.println("Current JVM version: " + System.getProperty("java.version"));
        Map<String, String> argsMap = parseArgs(args);
        System.out.println("Parsed arguments: " + argsMap);

        ApplicationManager app = new ApplicationManager();

        if (argsMap.containsKey("debug")) {
            DebugMode.setMode(DebugMode.SIMPLE_INFO);
        }

        boolean headless = argsMap.containsKey("headless") 
                            && Boolean.parseBoolean(argsMap.get("headless"));

        if (headless) {
            app.setOnlyHeadless();
            if (argsMap.containsKey("simulation")) {
                System.out.println("Loading simulation: " + argsMap.get("simulation"));   
                if (argsMap.containsKey("save"))
                    app.setSimulation(new Simulation(argsMap.get("simulation"),
                                                     argsMap.get("save")));
                else
                    app.setSimulation(new Simulation(argsMap.get("simulation")));
            }
            else {
                app.setSimulation(new Simulation());
            }
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

        disposeSimulationIfPresent();

        System.exit(0);
    }

    public void setRemoteGraphics(RemoteGraphics remoteGraphics) {
        if (this.remoteGraphics != null) {
            this.remoteGraphics.close();
        }
        this.remoteGraphics = remoteGraphics;
    }

    public RemoteGraphics getRemoteGraphics() {
        return remoteGraphics;
    }

    public boolean hasRemoteGraphics() {
        return remoteGraphics != null;
    }

    public void sendRemoteGraphics() {
        if (hasRemoteGraphics()) {
            sendRemoteGraphicsRequested = true;
        }
    }

    public void setSimulation(Simulation simulation) {
        disposeSimulationIfPresent();
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

    public void setOnlyHeadless() {
        onlyHeadless = true;
        switchToHeadlessMode();
    }

    public boolean isOnlyHeadless() {
        return onlyHeadless;
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

    public void handleRemoteGraphicsSendRequest() {
        try {
            remoteGraphics.send();
            sendRemoteGraphicsRequested = false;
            return;
        } catch (Exception e) {
            System.out.println("Failed to send remote graphics: " + e.getMessage());
        }
        System.out.println("Trying to rebuild remote graphics and send again...");
        remoteGraphics.rebuildClient();
        try {
            remoteGraphics.send();
        } catch (Exception e) {
            System.out.println("Failed again to send remote graphics: " + e.getMessage());
        }
    }

    public void update() {
        if(ApplicationManager.window == 0)
            ApplicationManager.window = glfwGetCurrentContext();

        if (hasSimulation() && simulation.isReady()) {

            if (hasRemoteGraphics() && sendRemoteGraphicsRequested) {
                handleRemoteGraphicsSendRequest();
            }

            simulation.update();
        }
        if (simulation.isFinished()) {
            System.out.println("Simulation finished.");
            exit();
        }
    }

    public void saveAndCloseCurrentSimulation() {
        if (hasSimulation())
            simulation.close();
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

    public GraphicsAdapter getGraphics() {
        return graphics;
    }

    public void toggleGraphics() {
        if (isGraphicsActive())
            switchToHeadlessMode();
        else
            headless = false;
    }

    public static boolean launchedWithDebug() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().
                getInputArguments().toString().contains("-agentlib:jdwp");
    }

    public void createGraphics() {
        headless = false;
        saveOnExit = false;

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        boolean isDebug = launchedWithDebug();

        if (isDebug)
            DebugMode.setMode(DebugMode.SIMPLE_INFO);

        if (isDebug | windowed | borderlessWindowed) {
            config.setWindowedMode(1920, 1080);
//        } else if (borderlessWindowed) {
//            Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
//            Gdx.graphics.setUndecorated(true);
//            Gdx.graphics.setWindowedMode(displayMode.width, displayMode.height);
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

    public void disposeSimulationIfPresent() {
        if (hasSimulation()) {
            simulation.dispose();
            simulation = null;
        }
    }
}
