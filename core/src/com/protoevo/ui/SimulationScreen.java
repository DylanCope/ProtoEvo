package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.protoevo.biology.neat.NeuralNetwork;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.core.Application;
import com.protoevo.core.Particle;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.WorldGenerationSettings;
import com.protoevo.env.Environment;
import com.protoevo.input.ParticleTracker;
import com.protoevo.ui.nn.NetworkRenderer;
import com.protoevo.ui.rendering.*;
import com.protoevo.ui.shaders.ShaderLayers;
import com.protoevo.ui.shaders.ShockWaveLayer;
import com.protoevo.ui.shaders.VignetteLayer;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.DebugMode;
import com.protoevo.utils.Utils;

import java.util.Map;
import java.util.TreeMap;

public class SimulationScreen {

    private final Simulation simulation;
    private final Environment environment;
    private final SimulationInputManager inputManager;
    private final Renderer renderer;
    private final SpriteBatch uiBatch;
    private final Stage stage;
    private final GlyphLayout layout = new GlyphLayout();
    private final OrthographicCamera camera;
    private final BitmapFont font, debugFont, titleFont;
    private final TopBar topBar;
    private final int infoTextSize, textAwayFromEdge;
    private final NetworkRenderer networkRenderer;
    private final float noRenderPollStatsTime = 2f;
    private float elapsedTime = 0, pollStatsCounter = 0, countDownToRender = 0;
    private final TreeMap<String, Float> stats = new TreeMap<>();
    private final TreeMap<String, Float> debugStats = new TreeMap<>();

    private float graphicsHeight;
    private float graphicsWidth;
    private boolean uiHidden = false, renderingEnabled = false, simLoaded = false;

    public static BitmapFont createFiraCode(int size) {
        String fontPath = "fonts/FiraCode-Retina.ttf";
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.local(fontPath));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.borderWidth = size / 10f;
        parameter.borderColor = new Color(0, 0, 0, .5f);
        return generator.generateFont(parameter);
    }

    public SimulationScreen(Application app, Simulation simulation) {
        CursorUtils.setDefaultCursor();

        graphicsHeight = Gdx.graphics.getHeight();
        graphicsWidth = Gdx.graphics.getWidth();

        camera = new OrthographicCamera();
        camera.setToOrtho(
                false, WorldGenerationSettings.environmentRadius,
                WorldGenerationSettings.environmentRadius * graphicsHeight / graphicsWidth);
        camera.position.set(0, 0, 0);
        camera.zoom = 1f; //WorldGenerationSettings.environmentRadius;

        this.simulation = simulation;
        this.environment = simulation.getEnv();
        stage = new Stage();
        uiBatch = new SpriteBatch();

        infoTextSize = (int) (graphicsHeight / 50f);
        textAwayFromEdge = (int) (graphicsWidth / 60);

        font = createFiraCode(infoTextSize);
        font.setColor(Color.WHITE.mul(.9f));
        debugFont = createFiraCode(infoTextSize);
        debugFont.setColor(Color.GOLD);

        titleFont = createFiraCode((int) (graphicsHeight / 40f));

        topBar = new TopBar(this, font.getLineHeight());

        ImageButton closeButton = createBarImageButton("icons/x-button.png", event -> {
            if (event.toString().equals("touchDown")) {
                Gdx.app.exit();
            }
            return true;
        });
        topBar.addRight(closeButton);

        ImageButton pauseButton = createBarImageButton("icons/play_pause.png", event -> {
            if (event.toString().equals("touchDown")) {
                simulation.togglePause();
            }
            return true;
        });
        topBar.addLeft(pauseButton);

        ImageButton toggleRenderingButton = createBarImageButton("icons/fast_forward.png", event -> {
            if (event.toString().equals("touchDown")) {
                toggleEnvironmentRendering();
                app.toggleSeparateThread();
            }
            return true;
        });
        topBar.addLeft(toggleRenderingButton);

        ImageButton homeButton = createBarImageButton("icons/home_icon.png", event -> {
            if (event.toString().equals("touchDown")) {
                camera.position.set(0, 0, 0);
                camera.zoom = WorldGenerationSettings.environmentRadius;
            }
            return true;
        });
        topBar.addLeft(homeButton);

        inputManager = new SimulationInputManager(this);
        renderer = new ShaderLayers(
                new EnvironmentRenderer(camera, simulation, inputManager),
                new ShockWaveLayer(camera),
                new VignetteLayer(camera, inputManager.getParticleTracker())
        );


        float boxWidth = (graphicsWidth / 2.0f - 1.2f * graphicsHeight * .4f);
        float boxHeight = 3 * graphicsHeight / 4;
        float boxXStart = graphicsWidth - boxWidth * 1.1f;
        float boxYStart = (graphicsHeight - boxHeight) / 2;
        networkRenderer = new NetworkRenderer(simulation, this,
                boxXStart, boxYStart, boxWidth, boxHeight, infoTextSize);
    }

    public ImageButton createImageButton(String texturePath, float width, float height, EventListener listener) {
        Texture texture = new Texture(texturePath);
        Drawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton button = new ImageButton(drawable);
        button.setSize(width, height);
        button.setTouchable(Touchable.enabled);
        button.addListener(listener);
        stage.addActor(button);
        return button;
    }

    public ImageButton createBarImageButton(String texturePath, EventListener listener) {
        return createImageButton(texturePath, topBar.getButtonSize(), topBar.getButtonSize(), listener);
    }

    public Stage getStage() {
        return stage;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public TopBar getTopBar() {
        return topBar;
    }

    public void drawDebugInfo() {

        String separator = " | ";
        String debugString = "FPS: " + Gdx.graphics.getFramesPerSecond();
        debugString += separator + "Zoom: " + ((int) (100 * camera.zoom)) / 100.f;
        debugString += separator + "Pos: " + Utils.numberToString(camera.position.x, 2)
                + ", " + Utils.numberToString(camera.position.y, 2);
        if (DebugMode.isDebugModePhysicsDebug()) {
            debugString += separator + "Bodies: " + environment.getWorld().getBodyCount();
            debugString += separator + "Contacts: " + environment.getWorld().getContactCount();
            debugString += separator + "Joints: " + environment.getWorld().getJointCount();
            debugString += separator + "Fixtures: " + environment.getWorld().getFixtureCount();
//            debugString += separator + "Proxies: " + environment.getWorld().getProxyCount();

            int totalCells = environment.getCells().size();
            int sleepCount = totalCells - (int) environment.getCells().stream()
                    .filter(cell -> cell.getBody().isAwake())
                    .count();
            debugString += separator + "Sleeping %: " + (int) (100f * sleepCount / totalCells);

            ParticleTracker tracker = inputManager.getParticleTracker();
            if (tracker.isTracking()) {
                Particle trackedParticle = tracker.getTrackedParticle();
                Map<String, Float> stats = trackedParticle.getDebugStats();
                int lineNumber = 0;
                int valueLength = 8;
                for (Map.Entry<String, Float> entityStat : stats.entrySet()) {
                    String valueStr = Utils.numberToString(entityStat.getValue(), 2);
                    String text = entityStat.getKey() + ": ";
                    for (int i = 0; i < valueLength - valueStr.length(); i++) {
                        text += " ";
                    }
                    text += valueStr;
                    layout.setText(debugFont, text);
                    float x = graphicsWidth - layout.width - textAwayFromEdge;
                    debugFont.draw(uiBatch, text, x, getYPosRHS(lineNumber));
                    lineNumber++;
                }
            }

        }
        debugFont.draw(uiBatch, debugString, 2 * topBar.getPadding(), font.getLineHeight() + topBar.getPadding());
    }

    public float getYPosLHS(int i) {
        return graphicsHeight - (1.3f*infoTextSize*i + 3 * graphicsHeight / 20f);
    }

    public float getYPosRHS(int i) {
        return graphicsHeight - topBar.getHeight() * 1.5f - 1.3f * infoTextSize * i;
    }

    private int renderStats(Map<String, Float> stats, int lineNumber, BitmapFont statsFont) {
        for (Map.Entry<String, Float> entityStat : stats.entrySet()) {
            String text = entityStat.getKey() + ": " + Utils.numberToString(entityStat.getValue(), 2);
            statsFont.draw(uiBatch, text, textAwayFromEdge, getYPosLHS(lineNumber));
            lineNumber++;
        }
        return lineNumber;
    }

    public int renderStats(Map<String, Float> stats) {
        return renderStats(stats, 0, font);
    }

    public void renderStats() {
        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (renderingEnabled && particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            float titleY = (float) (getYPosLHS(0) + 1.5 * titleFont.getLineHeight());
            titleFont.draw(uiBatch, particle.getPrettyName() + " Stats", textAwayFromEdge, titleY);
        } else {
            float titleY = (float) (getYPosLHS(0) + 1.5 * titleFont.getLineHeight());
            titleFont.draw(uiBatch, "Simulation Stats", textAwayFromEdge, titleY);
        }
        int lineNo = renderStats(stats);


        if (renderingEnabled && particleTracker.isTracking() &&
                particleTracker.getTrackedParticle() instanceof Protozoan) {
            Protozoan protozoan = (Protozoan) particleTracker.getTrackedParticle();
            int i = 0;
            for (SurfaceNode node : protozoan.getSurfaceNodes()) {
                if (node.getAttachment().isPresent()) {
                    float y = getYPosLHS(lineNo);
                    String text = "Node " + i + ": " + node.getAttachment().get().getName();
                    font.draw(uiBatch, text, textAwayFromEdge, y);
                    lineNo++;
                }
                i++;
            }
        }

        if (renderingEnabled && DebugMode.isDebugMode())
            renderStats(debugStats, lineNo, debugFont);
    }

    public void loadingString(String text) {
        uiBatch.begin();
        float x = 4 * topBar.getPadding() + topBar.getHeight();
        String textWithDots = text + ".".repeat(Math.max(0, (int) (elapsedTime * 2) % 4));
        font.draw(uiBatch, textWithDots, x, x);
        uiBatch.end();
    }

    public void draw(float delta) {
        elapsedTime += delta;

        if (countDownToRender > 0) {
            loadingString("Enabling Renderer");
            countDownToRender -= delta;
            if (countDownToRender <= 0) {
                renderingEnabled = true;
                simLoaded = true;
            }
        }

        camera.update();

        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (simLoaded && particleTracker.isTracking()) {
            if (particleTracker.getTrackedParticle().isDead())
                particleTracker.untrack();
            else
                camera.position.set(particleTracker.getTrackedParticlePosition());
        }

        if (simLoaded && renderingEnabled)
            renderer.render(delta);

        if (uiHidden)
            return;

        if (!simLoaded && countDownToRender <= 0) {
            loadingString("Loading Simulation");
            return;
        }
        else if (!renderingEnabled && countDownToRender <= 0) {
            loadingString("Accelerating Simulation");
        }

        topBar.draw(delta);

        uiBatch.begin();
        stage.act(delta);
        stage.draw();

        if (!renderingEnabled) {
            pollStatsCounter += delta;
            if (pollStatsCounter > noRenderPollStatsTime) {
                pollStatsCounter = 0;
                pollStats();
            }
        } else {
            pollStats();
        }

        renderStats();

        if (renderingEnabled && DebugMode.isDebugMode())
            drawDebugInfo();

        uiBatch.end();

        if (renderingEnabled && particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            if (particle instanceof Protozoan) {
                NeuralNetwork grn = ((Protozoan) particle).getGeneExpressionFunction().getRegulatoryNetwork();
                networkRenderer.setNeuralNetwork(grn);
                networkRenderer.render(delta);
            }
        }
    }

    public void pollStats() {
        stats.clear();
        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (renderingEnabled && particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            stats.putAll(particle.getStats());
            if (DebugMode.isDebugModePhysicsDebug()) {
                debugStats.clear();
                debugStats.putAll(particle.getDebugStats());
            }

        } else {
            stats.putAll(simulation.getEnv().getStats());
            if (DebugMode.isDebugModePhysicsDebug()) {
                debugStats.clear();
                debugStats.putAll(simulation.getEnv().getDebugStats());
            }
        }
    }

    public void dispose() {
        stage.dispose();
        uiBatch.dispose();
        font.dispose();
        topBar.dispose();
        renderer.dispose();
    }

    public boolean overOnScreenControls(int screenX, int screenY) {
        return topBar.pointOnBar(screenX, screenY);
    }

    public SimulationInputManager getInputManager() {
        return inputManager;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void toggleUI() {
        uiHidden = !uiHidden;
    }

    public void toggleEnvironmentRendering() {
        if (!renderingEnabled) {
            countDownToRender = 3;
            Gdx.graphics.setForegroundFPS(60);
        } else {
            renderingEnabled = false;
            Gdx.graphics.setForegroundFPS(5);
        }
    }

    public boolean hasSimulationNotLoaded() {
        return !simLoaded;
    }

    public synchronized void notifySimulationLoaded() {
        countDownToRender = 3;
    }
}
