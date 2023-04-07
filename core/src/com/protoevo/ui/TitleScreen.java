package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.core.Simulation;
import com.protoevo.ui.rendering.EnvironmentRenderer;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.DebugMode;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class TitleScreen extends ScreenAdapter {

    private final GraphicsAdapter graphics;
    private final Stage stage;
    private final VerticalGroup container;
    private final Label title, paddingLabel;
    private final List<TextButton> buttons = new ArrayList<>();

    public TitleScreen(GraphicsAdapter graphics) {
        this.graphics = graphics;

        stage = new Stage();
        stage.setDebugAll(DebugMode.isDebugMode());

        container = new VerticalGroup();
        container.center();
        stage.addActor(container);

        title = new Label("ProtoEvo", graphics.getSkin(), "mainTitle");
        paddingLabel = new Label("", graphics.getSkin());
    }

    private void createButtons() {

        buttons.clear();

        TextButton newSimulationButton = new TextButton("New Simulation", graphics.getSkin());
        newSimulationButton.addListener(e -> {
            if (e.toString().equals("touchDown"))
                graphics.setScreen(new CreateSimulationScreen(graphics));
            return true;
        });
        buttons.add(newSimulationButton);

//        if (DebugMode.isDebugMode()) {
//            TextButton sandboxButton = new TextButton("Start Debug Sandbox", graphics.getSkin());
//            sandboxButton.addListener(e -> {
//                if (e.toString().equals("touchDown"))
//                    graphics.moveToSandbox();
//                return true;
//            });
//            buttons.add(sandboxButton);
//        }

        try (Stream<Path> paths = Files.list(Paths.get("saves"))) {
            paths.map(dir -> dir.getName(dir.getNameCount() - 1).toString())
                    .sorted(Comparator.comparingLong(s -> -getMostRecentSaveModifiedTime(s)))
                    .limit(5)
                    .forEach(saveName -> {
                        TextButton button = new TextButton("Load " + saveName, graphics.getSkin());
                        button.addListener(e -> {
                            if (e.toString().equals("touchDown"))
                                graphics.moveToLoadSaveScreen(saveName);
                            return true;
                        });
                        buttons.add(button);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TextButton exitButton = new TextButton("Exit", graphics.getSkin());
        exitButton.addListener(e -> {
            if (e.toString().equals("touchDown"))
                graphics.exitApplication();
            return true;
        });
        buttons.add(exitButton);
    }

    public Long getMostRecentSaveModifiedTime(String saveName) {
        return Simulation.getMostRecentSave(saveName)
                .map(s -> s.toFile().lastModified()).orElseGet(() -> 0L);
    }

    @Override
    public void render(float delta) {
        GraphicsAdapter.renderBackground(delta);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {

        float y = 4 * title.getHeight();
        title.setPosition(width / 2f - title.getWidth() / 2f, height - y);
        stage.addActor(title);

        // center the container in the middle of the screen
        container.setBounds(0, title.getHeight(), width, height - y);
        container.clear();

        for (TextButton button : buttons)
            button.pad(button.getHeight() / 3f);

        container.addActor(paddingLabel);

        for (TextButton button : buttons)
            container.addActor(button);
    }

    @Override
    public void show() {
        DefaultBackgroundRenderer.getInstance().resumeSimulation();
        CursorUtils.setDefaultCursor();
        createButtons();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
