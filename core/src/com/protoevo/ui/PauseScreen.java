package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.settings.EnvironmentSettings;
import com.protoevo.settings.Settings;
import com.protoevo.utils.DebugMode;
import com.protoevo.utils.Utils;

public class PauseScreen extends ScreenAdapter {

    private final SimulationScreen simulationScreen;
    private final FrameBuffer fbo;
    private final SpriteBatch batch;
    private final ShaderProgram shader;
    private final GraphicsAdapter graphics;
    private final Stage stage;
    private float timePaused = 0;
    private final float fadeTime = 0.15f;

    public PauseScreen(GraphicsAdapter graphics, SimulationScreen simulationScreen) {
        this.simulationScreen = simulationScreen;
        this.graphics = graphics;

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        batch = new SpriteBatch();
        shader = new ShaderProgram(
                Gdx.files.internal("shaders/pause/vertex.glsl"),
                Gdx.files.internal("shaders/pause/fragment.glsl"));
        if (!shader.isCompiled()) {
            throw new RuntimeException("Shader compilation failed: " + shader.getLog());
        }

        stage = new Stage();
        stage.setDebugAll(DebugMode.isDebugMode());

        EnvironmentSettings settings = Environment.settings;

        final Table scrollTable = new Table();

        addSettingsButton(graphics, "General", settings, scrollTable);
        addSettingsButton(graphics, "Protozoa", settings.protozoa, scrollTable);
        addSettingsButton(graphics, "Plant", settings.plant, scrollTable);
        addSettingsButton(graphics, "Misc", settings.misc, scrollTable);

        addButton(graphics, "Exit to Title Screen", this::exitToTitleScreen, scrollTable);
        addButton(graphics, "Return to Simulation", this::returnToSimulation, scrollTable);

        final com.badlogic.gdx.scenes.scene2d.ui.ScrollPane scroller =
                new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(scrollTable);
        scroller.setScrollbarsVisible(true);
        scroller.setScrollbarsVisible(true);

        final Table table = new Table();

        final com.badlogic.gdx.scenes.scene2d.ui.Label nameText = new Label(
                "Paused", graphics.getSkin(), "mainTitle");
        nameText.setAlignment(Align.center);
        nameText.setWrap(true);

        table.setFillParent(true);
        table.add(nameText)
                .width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() / 7f).row();
        table.add(scroller)
                .width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() * 3 / 7f).row();

        stage.addActor(table);
    }

    private void exitToTitleScreen() {
        returnToSimulation();
        Simulation simulation = simulationScreen.getSimulation();
        simulation.onOtherThread(simulation::close);
        simulationScreen.addConditionalTask(
                () -> !simulation.isBusyOnOtherThread(),
                () -> graphics.moveToTitleScreen(simulationScreen)
        );
    }

    @Override
    public void show() {
        timePaused = 0;
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    public void addButton(GraphicsAdapter graphics, String text, Runnable action, Table table) {
        final TextButton button = new TextButton(text, graphics.getSkin());
        button.addListener(e -> {
            if (e.toString().equals("touchDown"))
                action.run();
            return true;
        });
        button.pad(button.getHeight() * .5f);
        table.add(button).padBottom(button.getHeight() / 4f).row();
    }

    public void addSettingsButton(GraphicsAdapter graphics, String name, Settings settings, Table table) {
        final TextButton button = new TextButton("Edit " + name + " Settings", graphics.getSkin());
        button.addListener(e -> {
            if (e.toString().equals("touchDown")) {
                EditSettingsScreen editSettingsScreen =
                        new EditSettingsScreen(this, graphics, name, settings);
                editSettingsScreen.setBackgroundRenderer(this::drawBackground);
                graphics.setScreen(editSettingsScreen);
            }
            return true;
        });
        button.pad(button.getHeight() * .5f);
        table.add(button).padBottom(button.getHeight() / 4f).row();
    }

    private void returnToSimulation() {
        simulationScreen.getSimulation().setPaused(false);
        graphics.setScreen(simulationScreen);
    }

    private void drawBackground(float delta) {
        FrameBufferManager fboManager = FrameBufferManager.getInstance();
        fboManager.begin(fbo);
        simulationScreen.renderEnvironment(delta);
        fboManager.end();

        Sprite sprite = new Sprite(fbo.getColorBufferTexture());
        sprite.flip(false, true);

        shader.bind();
        shader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shader.setUniformf("u_blurAmount",
                Utils.clampedLinearRemap(timePaused, 0, fadeTime, 0, 6f));
        shader.setUniformf("u_darkenAmount",
                Utils.clampedLinearRemap(timePaused, 0, fadeTime, 1f, 0.75f));
        batch.setShader(shader);

        batch.begin();
        batch.draw(sprite, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }

    public void setTimePaused(float t) {
        timePaused = t;
    }

    public float getTimePaused() {
        return timePaused;
    }

    public float getFadeTime() {
        return fadeTime;
    }

    @Override
    public void render(float delta) {
        timePaused += delta;

        drawBackground(delta);

        stage.act(delta);
        stage.draw();
    }

}
