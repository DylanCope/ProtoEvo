package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.*;
import com.protoevo.env.EnvFileIO;
import com.protoevo.physics.Particle;
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
import java.util.Set;
import java.util.concurrent.Callable;

public class SimulationScreen extends ScreenAdapter {

    private final GraphicsAdapter graphics;
    private final Simulation simulation;
    private final Environment environment;
    private final SimulationInputManager inputManager;
    private final Renderer renderer;
    private final SpriteBatch uiBatch;
    private final Stage stage;
    private final GlyphLayout layout = new GlyphLayout();
    private final OrthographicCamera camera;
    private final BitmapFont font, debugFont, statsTitle;
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
    private final ImageButton saveTrackedParticleButton;
    private final TextField saveTrackedParticleTextField;
    private final Set<ImageButton> buttons = new java.util.HashSet<>();

    private final SelectBox<String> statsSelectBox;
    private final float graphicsHeight;
    private final float graphicsWidth;
    private boolean uiHidden = false, renderingEnabled = false, simLoaded = false;

    public SimulationScreen(GraphicsAdapter graphics, Simulation simulation) {
        this.graphics = graphics;
        this.simulation = simulation;
        this.environment = simulation.getEnv();

        CursorUtils.setDefaultCursor();

        getStats = simulation.getEnv()::getStats;

        graphicsHeight = Gdx.graphics.getHeight();
        graphicsWidth = Gdx.graphics.getWidth();

        camera = new OrthographicCamera();
        camera.setToOrtho(
                false, WorldGenerationSettings.environmentRadius,
                WorldGenerationSettings.environmentRadius * graphicsHeight / graphicsWidth);
        camera.position.set(0, 0, 0);
        camera.zoom = 1f;

        stage = new Stage();
        uiBatch = new SpriteBatch();

        Skin skin = graphics.getSkin();

        stage.getRoot().addCaptureListener(event -> {
            if (stage.getKeyboardFocus() instanceof TextField
                    && !(event.getTarget() instanceof TextField))
                stage.setKeyboardFocus(null);
            return false;
        });

        infoTextSize = (int) (graphicsHeight / 50f);
        textAwayFromEdge = (int) (graphicsWidth / 60);

        font = skin.getFont("default");
        debugFont = skin.getFont("debug");

        statsTitle = skin.getFont("statsTitle");

        topBar = new TopBar(stage, font.getLineHeight());
        inputManager = new SimulationInputManager(this);

        saveTrackedParticleTextField = new TextField("", skin);
        stage.addActor(saveTrackedParticleTextField);
        saveTrackedParticleTextField.setVisible(false);
        saveTrackedParticleTextField.setMessageText("Save as...");

        saveTrackedParticleButton = createImageButton(
                "icons/save.png", topBar.getButtonSize(), topBar.getButtonSize(), event -> {
            if (event.toString().equals("touchDown")) {
                if (trackedParticle != null) {
                    EnvFileIO.saveCell((Cell) trackedParticle,  saveTrackedParticleTextField.getText());
                }
            }
            return true;
        });
        saveTrackedParticleButton.setVisible(false);

        statsSelectBox = new SelectBox<>(skin, "statsTitle");
        stage.addActor(statsSelectBox);
        statsSelectBox.setHeight(statsSelectBox.getStyle().font.getLineHeight());
        setEnvStatOptions();

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

    @Override
    public void show() {
        super.show();
        inputManager.registerAsInputProcessor();
    }

    @Override
    public void hide() {
        super.hide();
        Gdx.input.setInputProcessor(null);
    }

    public ImageButton createImageButton(String texturePath, float width, float height, EventListener listener) {
        Texture texture = ImageUtils.getTexture(texturePath);
        Drawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton button = new ImageButton(drawable);
        button.setSize(width, height);
        button.setTouchable(Touchable.enabled);
        button.addListener(listener);
        stage.addActor(button);
        buttons.add(button);
        return button;
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
            for (int i = 0; i < Math.max(0, maxLength - valueStr.length()); i++)
                text.append(" ");
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

    public void setSaveParticleTopBarUI() {
        saveTrackedParticleButton.setVisible(true);
        float fieldWidthMul = 8f;

        Vector2 pos = topBar.nextLeftPosition();
        saveTrackedParticleButton.setPosition(pos.x, pos.y);
        saveTrackedParticleTextField.setVisible(true);
        saveTrackedParticleTextField.setBounds(
                saveTrackedParticleButton.getX() + saveTrackedParticleButton.getWidth() * 1.3f,
                saveTrackedParticleButton.getY(),
                fieldWidthMul * saveTrackedParticleButton.getWidth(),
                saveTrackedParticleButton.getHeight()
        );
    }

    public void setProtozoaStatOptions(Protozoan protozoan) {
        statGetters.clear();
        ArrayList<String> statOptions = new ArrayList<>();
        statOptions.add("Protozoan Stats");
        statGetters.put("Protozoan Stats", protozoan::getStats);
        getStats = protozoan::getStats;

        layout.setText(statsSelectBox.getStyle().font, "Protozoan Stats");
        float maxWidth = layout.width;

        for (SurfaceNode node : protozoan.getSurfaceNodes()) {
            String option;
            if (node.getAttachment() != null)
                option = "Node " + node.getIndex() + " (" + node.getAttachmentName() + ") Stats";
            else
                option = "Node " + node.getIndex() + " Stats";
            statOptions.add(option);
            statGetters.put(option, node::getStats);
            layout.setText(statsSelectBox.getStyle().font, option);
            maxWidth = Math.max(maxWidth, layout.width);
        }

        for (Organelle organelle : protozoan.getOrganelles()) {
            String option;
            if (organelle.getFunction() != null)
                option = "Organelle " + organelle.getIndex()
                        + " (" + organelle.getFunction().getName() + ") Stats";
            else
                option = "Organelle " + organelle.getIndex() + " Stats";
            statOptions.add(option);
            statGetters.put(option, organelle::getStats);
            layout.setText(statsSelectBox.getStyle().font, option);
            maxWidth = Math.max(maxWidth, layout.width);
        }

        statsSelectBox.setItems(statOptions.toArray(new String[0]));
        statsSelectBox.setWidth(maxWidth);
        statsSelectBox.setSelected("Protozoan Stats");
    }

    public void addStatOption(String name, Callable<Statistics> getter) {
        statGetters.put(name, getter);
        layout.setText(statsSelectBox.getStyle().font, name);
        if (layout.width > statsSelectBox.getWidth())
            statsSelectBox.setWidth(layout.width);
        statsSelectBox.setItems(statGetters.keySet().toArray(new String[0]));
    }

    public void setEnvStatOptions() {
        statGetters.clear();
        addStatOption("Simulation Stats", environment::getStats);
        addStatOption("Protozoa Summary", environment::getProtozoaSummaryStats);
        statsSelectBox.setSelected("Simulation Stats");
    }

    public void hideSaveParticleTopBarUI() {
        saveTrackedParticleButton.setVisible(false);
        saveTrackedParticleTextField.setVisible(false);
    }

    public void renderStats() {
        float titleY = (float) (17 * graphicsHeight / 20f + 1.5 * statsTitle.getLineHeight());

        if (statsSelectBox.getSelected() != null && statsSelectBox.isVisible())
            getStats = statGetters.get(statsSelectBox.getSelected());

        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (renderingEnabled && particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();

            if ((trackedParticle != particle)) {
                if (particle instanceof Protozoan) {
                    setSaveParticleTopBarUI();
                    setProtozoaStatOptions((Protozoan) particle);
                } else {
                    hideSaveParticleTopBarUI();
                    statGetters.clear();
                    addStatOption(particle.getPrettyName() + " Stats", particle::getStats);
                    statsSelectBox.setSelectedIndex(0);
                }
                trackedParticle = particle;
            }
        }
        else if (trackedParticle != null) {
            trackedParticle = null;
            hideSaveParticleTopBarUI();
            setEnvStatOptions();
        }

        statsSelectBox.setPosition(textAwayFromEdge, titleY - statsSelectBox.getStyle().font.getLineHeight() / 2f);

        renderStats(stats);
    }

    public void loadingString(String text) {
        uiBatch.begin();
        float x = 4 * topBar.getPadding() + topBar.getHeight();
        StringBuilder textWithDots = new StringBuilder(text);
        for (int i = 0; i < Math.max(0, (int) (elapsedTime * 2) % 4); i++)
            textWithDots.append(".");

        font.draw(uiBatch, textWithDots.toString(), x, x);
        uiBatch.end();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(EnvironmentRenderer.backgroundColor);

        if (simLoaded)
            simulation.update();

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
//        font.dispose();
//        statsTitle.dispose();
//        debugFont.dispose();
        topBar.dispose();
        renderer.dispose();
        networkRenderer.dispose();
        inputManager.dispose();
        for (ImageButton button : buttons)
            ((TextureRegionDrawable) button.getImage().getDrawable()).getRegion().getTexture().dispose();
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

    public GraphicsAdapter getGraphics() {
        return graphics;
    }
}
