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

public class EditSettingsScreen extends ScreenAdapter {
    private final Stage stage;
    private final GraphicsAdapter graphics;
    private final String simulationName;
    private final Skin skin;

    public EditSettingsScreen(ScreenAdapter previousScreen, GraphicsAdapter graphics, String settingsName, Settings settings) {
        this.graphics = graphics;
        this.simulationName = settingsName;

        this.stage = new Stage();
        stage.setDebugAll(DebugMode.isDebugMode());

        TopBar topBar = new TopBar(this.stage, graphics.getSkin().getFont("default").getLineHeight());

        topBar.createRightBarImageButton("icons/back.png", () -> {
            graphics.setScreen(previousScreen);
        });

        skin = graphics.getSkin();
        final Table scrollTable = new Table();

        final Map<Settings.Parameter<?>, TextField> parameterFields = new HashMap<>();

        for (Settings.Parameter<?> parameter : settings.getParameters()) {
            if (parameter.getName() == null || parameter.getName().equals(""))
                continue;

            TextField field = createParameterInput(parameter, scrollTable);
            parameterFields.put(parameter, field);
        }

        final ScrollPane scroller = new ScrollPane(scrollTable);
        scroller.setScrollbarsVisible(true);
        scroller.setScrollbarsVisible(true);

        final Table table = new Table();

        final Label nameText = new Label(settingsName + " Settings", skin, "mainTitle");
        nameText.setAlignment(Align.center);
        nameText.setWrap(true);

        table.setFillParent(true);
        table.add(nameText).width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() / 5f).row();
        table.add(scroller).width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() * 4 / 7f).row();

        final TextButton applyButton = new TextButton("Apply", skin);
        applyButton.addListener(e -> {
            if (e.toString().equals("touchDown")) {
                for (Map.Entry<Settings.Parameter<?>, TextField> entry : parameterFields.entrySet()) {
                    Settings.Parameter<?> parameter = entry.getKey();
                    TextField field = entry.getValue();
                    if (field.getText().equals(parameter.get().toString())) {
                        continue;
                    }
                    try {
                        parameter.set(field.getText());
                    } catch (RuntimeException ex) {
                        System.err.println(
                                "Failed to set parameter " + parameter.getName() + " to " + field.getText());
                    }
                }
                graphics.setScreen(previousScreen);
            }
            return true;
        });

        table.add(applyButton).padTop(applyButton.getHeight()).width(applyButton.getWidth() * 1.2f);

        this.stage.addActor(table);
    }

    private TextField createParameterInput(Settings.Parameter<?> parameter, Table table) {
        float scrollWidth = Gdx.graphics.getWidth() / 2f;
        final Label label = new Label(parameter.getName() + ": ", skin);
        final TextField textField = new TextField(parameter.get().toString(), skin);
        final Table inputTable = new Table();

        inputTable.add(label)
                .width(scrollWidth / 2f)
                .align(Align.right);
        inputTable.add(textField)
                .width(scrollWidth / 2f)
                .align(Align.left)
                .row();

        table.add(inputTable)
                .padBottom(label.getHeight() / 2f).row();
        return textField;
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
