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
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Application;
import com.protoevo.core.Particle;
import com.protoevo.core.Simulation;
import com.protoevo.core.Statistics;
import com.protoevo.settings.WorldGenerationSettings;
import com.protoevo.env.Environment;
import com.protoevo.input.ParticleTracker;
import com.protoevo.ui.nn.MouseOverNeuronCallback;
import com.protoevo.ui.nn.NetworkRenderer;
import com.protoevo.ui.rendering.*;
import com.protoevo.ui.shaders.ShaderLayers;
import com.protoevo.ui.shaders.ShockWaveLayer;
import com.protoevo.ui.shaders.VignetteLayer;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.DebugMode;
import com.protoevo.utils.ImageUtils;
import com.protoevo.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

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
    private final MouseOverNeuronCallback mouseOverNeuronCallback;
    private final float noRenderPollStatsTime = 2f;
    private float elapsedTime = 0, pollStatsCounter = 0, countDownToRender = 0;
    private float graphicsStatsYOffset = 0;
    private final Statistics stats = new Statistics();
    private Callable<Statistics> getStats;
    private final Map<String, Callable<Statistics>> statGetters = new HashMap<>();
    private final Statistics debugStats = new Statistics();
    private Particle trackedParticle;

    private final SelectBox<String> selectBox;

    private final float graphicsHeight;
    private final float graphicsWidth;
    private boolean uiHidden = false, renderingEnabled = false, simLoaded = false;

    public SimulationScreen(Application app, Simulation simulation) {
        CursorUtils.setDefaultCursor();

        statGetters.put("Env", () -> simulation.getEnv().getStats());
        getStats = statGetters.get("Env");

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

        font = UIStyle.createFiraCode(infoTextSize);
        font.setColor(Color.WHITE.mul(.9f));
        debugFont = UIStyle.createFiraCode(infoTextSize);
        debugFont.setColor(Color.GOLD);

        titleFont = UIStyle.createFiraCode((int) (graphicsHeight / 40f));

        topBar = new TopBar(this, font.getLineHeight());

        ImageButton closeButton = createBarImageButton("icons/x-button.png", event -> {
            simulation.close();
            Gdx.app.exit();
            System.exit(0);
            return true;
        });
        topBar.addRight(closeButton);

        ImageButton pauseButton = createBarImageButton("icons/play_pause.png", event -> {
            simulation.togglePause();
            return true;
        });
        topBar.addLeft(pauseButton);

        ImageButton toggleRenderingButton = createBarImageButton("icons/fast_forward.png", event -> {
            toggleEnvironmentRendering();
            app.toggleSeparateThread();
            return true;
        });
        topBar.addLeft(toggleRenderingButton);

        ImageButton homeButton = createBarImageButton("icons/home_icon.png", event -> {
            camera.position.set(0, 0, 0);
            camera.zoom = WorldGenerationSettings.environmentRadius;
            return true;
        });
        topBar.addLeft(homeButton);

        Skin skin = UIStyle.getUISkin();

        selectBox = new SelectBox<>(skin);
        selectBox.getStyle().font = titleFont;
        stage.addActor(selectBox);

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
        mouseOverNeuronCallback = new MouseOverNeuronCallback(font);
        networkRenderer = new NetworkRenderer(
                simulation, this, uiBatch, mouseOverNeuronCallback,
                boxXStart, boxYStart, boxWidth, boxHeight, infoTextSize);
    }

    public ImageButton createImageButton(String texturePath, float width, float height, EventListener listener) {
        Texture texture = ImageUtils.getTexture(texturePath);

        Drawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton button = new ImageButton(drawable);
        button.setSize(width, height);
        button.setTouchable(Touchable.enabled);
        button.addListener(listener);
        stage.addActor(button);
        return button;
    }

    public ImageButton createBarImageButton(String texturePath, EventListener touchListener) {
        return createImageButton(texturePath, topBar.getButtonSize(), topBar.getButtonSize(), event -> {
            if (event.toString().equals("touchDown")) {
                touchListener.handle(event);
            }
            return true;
        });
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

        ParticleTracker tracker = inputManager.getParticleTracker();
        debugStats.clear();
        if (tracker.isTracking()) {
            Particle trackedParticle = tracker.getTrackedParticle();
            debugStats.putAll(trackedParticle.getDebugStats());
        }
        else if (DebugMode.isDebugModePhysicsDebug())
            debugStats.putAll(environment.getPhysicsDebugStats());
        else
            debugStats.putAll(environment.getDebugStats());

        int lineNumber = 0;
        int maxLength = 0;
        for (Statistics.Stat entityStat : debugStats) {
            int statLen = entityStat.getValueString().length();
            maxLength = Math.max(maxLength, statLen);
        }
        maxLength += 3;

        for (Statistics.Stat entityStat : debugStats) {
            String valueStr = entityStat.getValueString();
            StringBuilder text = new StringBuilder(entityStat.getName() + ": ");
            text.append(" ".repeat(Math.max(0, maxLength - valueStr.length())));
            text.append(valueStr);
            layout.setText(debugFont, text.toString());
            float x = graphicsWidth - layout.width - textAwayFromEdge;
            debugFont.draw(uiBatch, text.toString(), x, getYPosRHS(lineNumber));
            lineNumber++;
        }
        debugFont.draw(uiBatch, debugString,
                2 * topBar.getPadding(), font.getLineHeight() + topBar.getPadding());
    }

    public float getYPosLHS(int i) {
        return graphicsHeight - (1.3f*infoTextSize*i + 3 * graphicsHeight / 20f) - graphicsStatsYOffset;
    }

    public float getYPosRHS(int i) {
        return graphicsHeight - topBar.getHeight() * 1.5f - 1.3f * infoTextSize * i;
    }

    private int renderStats(Statistics stats, int lineNumber, BitmapFont statsFont) {
        for (Statistics.Stat stat : stats) {
            statsFont.draw(uiBatch, stat.toString(), textAwayFromEdge, getYPosLHS(lineNumber));
            lineNumber++;
        }
        return lineNumber;
    }

    public int renderStats(Statistics stats) {
        return renderStats(stats, 0, font);
    }

    public void renderStats() {
        float titleY = (float) (17 * graphicsHeight / 20f + 1.5 * titleFont.getLineHeight());

        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (renderingEnabled && particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();

            if ((!selectBox.isVisible() || trackedParticle != particle) && particle instanceof Protozoan) {
                ArrayList<String> statOptions = new ArrayList<>();
                statOptions.add("Protozoan Stats");
                statGetters.put("Protozoan Stats", particle::getStats);
                getStats = particle::getStats;

                layout.setText(selectBox.getStyle().font, "Protozoan Stats");
                float maxWidth = layout.width;

                for (SurfaceNode node : ((Protozoan) particle).getSurfaceNodes()) {
                    String option;
                    if (node.getAttachment() != null)
                         option = "Node " + node.getIndex() + " (" + node.getAttachmentName() + ") Stats";
                    else
                        option = "Node " + node.getIndex() + " Stats";
                    statOptions.add(option);
                    statGetters.put(option, node::getStats);
                    layout.setText(selectBox.getStyle().font, option);
                    maxWidth = Math.max(maxWidth, layout.width);
                }

                for (Organelle organelle : ((Protozoan) particle).getOrganelles()) {
                    String option;
                    if (organelle.getFunction() != null)
                        option = "Organelle " + organelle.getIndex()
                                + " (" + organelle.getFunction().getName() + ") Stats";
                    else
                        option = "Organelle " + organelle.getIndex() + " Stats";
                    statOptions.add(option);
                    statGetters.put(option, organelle::getStats);
                    layout.setText(selectBox.getStyle().font, option);
                    maxWidth = Math.max(maxWidth, layout.width);
                }

                if (statOptions.size() >= 2) {
                    selectBox.setVisible(true);
                    selectBox.setBounds(
                            textAwayFromEdge, titleY - selectBox.getStyle().font.getLineHeight() / 2f,
                            maxWidth, selectBox.getStyle().font.getLineHeight());
                    selectBox.setItems(statOptions.toArray(new String[0]));
                }
            } else if (!(particle instanceof Protozoan)) {
                getStats = particle::getStats;
                selectBox.setVisible(false);
            }

            if (selectBox.getSelected() != null && selectBox.isVisible())
                getStats = statGetters.get(selectBox.getSelected());

            if (!selectBox.isVisible()) {
                titleFont.draw(uiBatch, particle.getPrettyName() + " Stats", textAwayFromEdge, titleY);
            }
            trackedParticle = particle;
        }
        else {
            getStats = statGetters.get("Env");
            selectBox.setVisible(false);
            graphicsStatsYOffset = 0;

            titleFont.draw(uiBatch, "Simulation Stats", textAwayFromEdge, titleY);
        }

        renderStats(stats);
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

        if (!renderingEnabled) {
            pollStatsCounter += delta;
            if (pollStatsCounter > noRenderPollStatsTime) {
                pollStatsCounter = 0;
                pollStats();
            }
        } else {
            pollStats();
        }

        handleNetworkRenderer(delta);

        renderStats();

        if (renderingEnabled && DebugMode.isDebugMode())
            drawDebugInfo();

        uiBatch.end();

        stage.act(delta);
        stage.draw();
    }

    private void handleNetworkRenderer(float delta) {
        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (renderingEnabled && particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            if (particle instanceof Protozoan) {
                Protozoan protozoan = (Protozoan) particle;
                NeuralNetwork grn = protozoan.getGeneExpressionFunction().getRegulatoryNetwork();
                if (grn != null) {
                    networkRenderer.setNeuralNetwork(grn);
                    mouseOverNeuronCallback.setGeneExpressionFunction(protozoan.getGeneExpressionFunction());
                    networkRenderer.render(delta);
                }
            }
        }
    }

    public void pollStats() {
        ParticleTracker particleTracker = inputManager.getParticleTracker();
        try {
            if (getStats == null)
                getStats = simulation.getEnv()::getStats;

            stats.clear();
            stats.putAll(getStats.call());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (renderingEnabled && particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            if (DebugMode.isDebugModePhysicsDebug()) {
                debugStats.clear();
                debugStats.putAll(particle.getDebugStats());
            }

        } else {
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
