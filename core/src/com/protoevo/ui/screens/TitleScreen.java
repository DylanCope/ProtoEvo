package com.protoevo.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.protoevo.core.ApplicationManager;
import com.protoevo.core.Simulation;
import com.protoevo.networking.RemoteSimulation;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.DebugMode;

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

        addButton("New Simulation", () -> graphics.setScreen(new CreateSimulationScreen(graphics)));
        addButton("Fork Remote Simulation", () -> graphics.loadPreexistingSimulation(new RemoteSimulation()));

        Path savesPath = Paths.get("saves");
        try {
            Files.createDirectories(savesPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (Stream<Path> paths = Files.list(savesPath)) {
            paths.map(dir -> dir.getName(dir.getNameCount() - 1).toString())
                    .sorted(Comparator.comparingLong(s -> -getMostRecentSaveModifiedTime(s)))
                    .limit(5)
                    .forEach(saveName -> {
                        addButton("Load " + saveName, () -> graphics.moveToLoadSaveScreen(saveName));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        addButton("Edit Settings", this::switchToEditSettingsScreen);
        addButton("Exit", graphics::exitApplication);
    }

    public void switchToEditSettingsScreen() {
        graphics.setScreen(new EditSettingsScreen(
                this, graphics, ApplicationManager.settings, null,
                () -> {
                    ApplicationManager manager = graphics.getManager();
                    manager.closeGraphics();
                    ApplicationManager.settings.save();
                }
        ));
    }

    private void addButton(String text, Runnable onClick) {
        TextButton button = new TextButton(text, graphics.getSkin());
        button.addListener(e -> {
            if (e.toString().equals("touchDown"))
                onClick.run();
            return true;
        });
        buttons.add(button);
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
