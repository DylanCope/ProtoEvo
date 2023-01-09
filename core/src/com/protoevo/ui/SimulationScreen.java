package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.protoevo.core.Particle;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.Settings;
import com.protoevo.env.Environment;
import com.protoevo.input.ParticleTracker;
import com.protoevo.ui.rendering.*;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.DebugMode;
import com.protoevo.utils.Utils;

import java.util.Map;

public class SimulationScreen {

    private final Simulation simulation;
    private final Environment environment;
    private final InputManager inputManager;
    private final Renderer renderer;
    private final SpriteBatch uiBatch;
    private final Stage stage;
    private final GlyphLayout layout = new GlyphLayout();
    private final OrthographicCamera camera;
    private final BitmapFont font, debugFont, titleFont;
    private final TopBar topBar;
    private final int infoTextSize, textAwayFromEdge;

    private float graphicsHeight;
    private float graphicsWidth;

    public static BitmapFont createFiraCode(int size) {
        String fontPath = "fonts/FiraCode-Retina.ttf";
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.local(fontPath));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.borderWidth = size / 10f;
        parameter.borderColor = new Color(0, 0, 0, .5f);
        return generator.generateFont(parameter);
    }

    public SimulationScreen(Simulation simulation) {
        CursorUtils.setDefaultCursor();

        graphicsHeight = Gdx.graphics.getHeight();
        graphicsWidth = Gdx.graphics.getWidth();

        camera = new OrthographicCamera();
//        camera.setToOrtho(false, 1, graphicsHeight / graphicsWidth);
        camera.setToOrtho(false, graphicsWidth, graphicsHeight);
        camera.position.set(0, 0, 0);
        camera.zoom = Math.max(graphicsWidth, graphicsHeight) / Settings.tankRadius;

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

        ImageButton homeButton = createBarImageButton("icons/home_icon.png", event -> {
            if (event.toString().equals("touchDown")) {
                camera.position.set(0, 0, 0);
                camera.zoom = 1;
            }
            return true;
        });
        topBar.addLeft(homeButton);

        inputManager = new InputManager(this);
        renderer = new ShaderLayers(
                new EnvironmentRenderer(camera, simulation, inputManager),
                new ShockWaveLayer(camera),
                new VignetteLayer(camera, inputManager.getParticleTracker())
        );
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
        debugString += separator + "Pos: " + (int) camera.position.x + ", " + (int) camera.position.y;
        if (DebugMode.isDebugModePhysicsDebug()) {
            debugString += separator + "Bodies: " + environment.getWorld().getBodyCount();
            debugString += separator + "Contacts: " + environment.getWorld().getContactCount();
            debugString += separator + "Joints: " + environment.getWorld().getJointCount();
            debugString += separator + "Fixtures: " + environment.getWorld().getFixtureCount();
            debugString += separator + "Proxies: " + environment.getWorld().getProxyCount();

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
                    float x = camera.viewportWidth - layout.width - textAwayFromEdge;
                    debugFont.draw(uiBatch, text, x, getYPosRHS(lineNumber));
                    lineNumber++;
                }
            }

        }
        debugFont.draw(uiBatch, debugString, 2 * topBar.getPadding(), font.getLineHeight() + topBar.getPadding());
    }

    public float getYPosLHS(int i) {
        return camera.viewportHeight - (1.3f*infoTextSize*i + 3 * camera.viewportHeight / 20f);
    }

    public float getYPosRHS(int i) {
        return 1.3f*infoTextSize*i + camera.viewportHeight / 20f;
    }

    private void renderStats(Map<String, Float> stats) {
        int lineNumber = 0;
        for (Map.Entry<String, Float> entityStat : stats.entrySet()) {
            String text = entityStat.getKey() + ": " + Utils.numberToString(entityStat.getValue(), 2);
            font.draw(uiBatch, text, textAwayFromEdge, getYPosLHS(lineNumber));
            lineNumber++;
        }
    }

    public void draw(float delta) {
        camera.update();
//        float factor = 2f;
//        camera.combined.mul(new Matrix4().setToScaling(2, 2, 0));
//        camera.invProjectionView.mul(new Matrix4().setToScaling(1 / factor, 1 / factor, 0));
        if (inputManager.getParticleTracker().isTracking())
            camera.position.set(inputManager.getParticleTracker().getTrackedParticlePosition());

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        renderer.render(delta);

        topBar.draw(delta);

        uiBatch.begin();
        stage.act(delta);
        stage.draw();

        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            float titleY = (float) (getYPosLHS(0) + 1.5 * titleFont.getLineHeight());
            titleFont.draw(uiBatch, particle.getPrettyName() + " Stats", textAwayFromEdge, titleY);
            renderStats(particle.getStats());
        } else {
            float titleY = (float) (getYPosLHS(0) + 1.5 * titleFont.getLineHeight());
            titleFont.draw(uiBatch, "Simulation Stats", textAwayFromEdge, titleY);
            renderStats(simulation.getEnv().getStats());
        }

        if (DebugMode.isDebugMode())
            drawDebugInfo();

        uiBatch.end();
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

    public InputManager getInputManager() {
        return inputManager;
    }
}
