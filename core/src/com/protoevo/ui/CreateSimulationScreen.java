package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.protoevo.core.Simulation;
import com.protoevo.settings.EnvironmentSettings;
import com.protoevo.settings.Settings;
import com.protoevo.ui.rendering.EnvironmentRenderer;
import com.protoevo.utils.DebugMode;
import com.protoevo.utils.FileIO;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CreateSimulationScreen extends ScreenAdapter {
    private final Stage stage;
    private final Skin skin;

    public CreateSimulationScreen(GraphicsAdapter graphics) {
        stage = new Stage();
        stage.setDebugAll(DebugMode.isDebugMode());

        EnvironmentSettings settings = EnvironmentSettings.createDefault();

        TopBar topBar = new TopBar(this.stage, graphics.getSkin().getFont("default").getLineHeight());

        topBar.createRightBarImageButton("icons/x-button.png", graphics::exitApplication);
        topBar.createRightBarImageButton("icons/back.png", () -> {
            graphics.moveToTitleScreen(this);
        });

        skin = graphics.getSkin();
        final Table scrollTable = new Table();

        final Label label = new Label("Simulation Name:", skin);
        label.setAlignment(Align.right);
        final TextField nameField = new TextField(Simulation.generateSimName(), skin);
        nameField.setAlignment(Align.center);
        scrollTable.add(label)
                .padBottom(label.getHeight() / 2f)
                .row();
        scrollTable.add(nameField)
                .width(2 * label.getWidth())
                .padBottom(nameField.getHeight() * 2f)
                .row();

        addSettingsButton(graphics, "World Generation", settings.world, scrollTable);
        addSettingsButton(graphics, "General", settings, scrollTable);
        addSettingsButton(graphics, "Protozoa", settings.protozoa, scrollTable);
        addSettingsButton(graphics, "Plant", settings.plant, scrollTable);
        addSettingsButton(graphics, "Misc", settings.misc, scrollTable);

        final ScrollPane scroller = new ScrollPane(scrollTable);
        scroller.setScrollbarsVisible(true);
        scroller.setScrollbarsVisible(true);

        final Table table = new Table();

        final Label nameText = new Label("New Simulation", skin, "mainTitle");
        nameText.setAlignment(Align.center);
        nameText.setWrap(true);

        table.setFillParent(true);
        table.add(nameText).width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() / 5f).row();
        table.add(scroller).width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() * 4 / 7f).row();

        final TextButton createButton = new TextButton("Create", skin);
        createButton.addListener(e -> {
            if (e.toString().equals("touchDown"))
                graphics.loadSimulation(new Simulation(nameField.getText(), settings));
            return true;
        });

        table.add(createButton).padTop(createButton.getHeight()).width(createButton.getWidth() * 1.2f);


        this.stage.addActor(table);
    }

    public void addSettingsButton(GraphicsAdapter graphics, String name, Settings settings, Table table) {
        final TextButton button = new TextButton("Edit " + name + " Settings", skin);
        button.addListener(e -> {
            if (e.toString().equals("touchDown"))
                graphics.setScreen(new EditSettingsScreen(this, graphics, name, settings));
            return true;
        });
        table.add(button).padBottom(button.getHeight() / 2f).row();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        this.stage.act(delta);
        ScreenUtils.clear(EnvironmentRenderer.backgroundColor);
        this.stage.draw();
    }

    @Override
    public void resize(final int width, final int height) {
        this.stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {}
}
