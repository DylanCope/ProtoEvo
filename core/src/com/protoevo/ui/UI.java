package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.Settings;
import com.protoevo.env.Environment;
import com.protoevo.input.MoveParticleButton;
import com.protoevo.ui.rendering.Renderer;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.DebugMode;

public class UI {

    private final Simulation simulation;
    private final Environment environment;
    private final InputManager inputManager;
    private final Renderer renderer;
    private final SpriteBatch uiBatch;
    private Stage stage;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private final TopBar topBar;

    public static BitmapFont createFiraCode(int size) {
        String fontPath = "fonts/FiraCode-Retina.ttf";
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.local(fontPath));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        return generator.generateFont(parameter);
    }

    public UI(Simulation simulation) {
        CursorUtils.setDefaultCursor();

        this.simulation = simulation;
        this.environment = simulation.getEnv();
        stage = new Stage();
        uiBatch = new SpriteBatch();

        font = createFiraCode(24);

        float height = Gdx.graphics.getHeight();
        float width = Gdx.graphics.getWidth();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, width, height);
        camera.position.set(0, 0, 0);
        camera.zoom = Math.max(width, height) / Settings.tankRadius;

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
        renderer = new Renderer(camera, simulation, inputManager);
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
        }
        font.setColor(Color.GOLD);
        font.draw(uiBatch, debugString, 2 * topBar.getPadding(), font.getLineHeight() + topBar.getPadding());
    }


    public void draw(float delta) {

        renderer.render(delta);

        topBar.draw(delta);

        uiBatch.begin();
        stage.draw();

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
