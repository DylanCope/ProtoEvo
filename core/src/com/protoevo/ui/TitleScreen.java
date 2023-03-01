package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.ui.rendering.EnvironmentRenderer;

public class TitleScreen extends ScreenAdapter {

    private final GraphicsAdapter graphics;
    private final Stage stage;
    private final VerticalGroup container;
    private final Label title, paddingLabel;
    private final TextButton sandboxButton, newSimulationButton;

    public TitleScreen(GraphicsAdapter graphics) {
        this.graphics = graphics;

        stage = new Stage();
        container = new VerticalGroup();
        container.center();
        stage.addActor(container);

        title = new Label("ProtoEvo", graphics.getSkin(), "mainTitle");
        paddingLabel = new Label("", graphics.getSkin());

        newSimulationButton = new TextButton("New Simulation", graphics.getSkin());
        newSimulationButton.addListener(e -> {
            if (e.toString().equals("touchDown"))
                graphics.moveToSimulationScreen();
            return true;
        });

        sandboxButton = new TextButton("Start Sandbox", graphics.getSkin());
        sandboxButton.addListener(e -> {
            if (e.toString().equals("touchDown"))
                graphics.moveToSandbox();
            return true;
        });
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(EnvironmentRenderer.backgroundColor);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // center the container in the middle of the screen
        container.setBounds(0, 0, width, height);
        container.clear();

        newSimulationButton.pad(newSimulationButton.getHeight() * .5f);
        sandboxButton.pad(sandboxButton.getHeight() * .5f);

        container.addActor(title);
        paddingLabel.setHeight(title.getHeight() * 2);
        container.addActor(paddingLabel);
        container.addActor(newSimulationButton);
        container.addActor(sandboxButton);
    }

    @Override
    public void show() {
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
