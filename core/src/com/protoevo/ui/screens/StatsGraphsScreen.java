package com.protoevo.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.protoevo.core.Simulation;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.ui.elements.Plot;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.DebugMode;

public class StatsGraphsScreen extends ScreenAdapter {

    private Simulation simulation;
    private boolean wasSimulationPaused;
    private GraphicsAdapter graphics;
    private final Stage stage;
    private final Plot plot;

    public StatsGraphsScreen(GraphicsAdapter graphics, Simulation simulation) {
        this.graphics = graphics;
        this.simulation = simulation;
        this.stage = new Stage();
        stage.setDebugAll(DebugMode.isDebugMode());

        final Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        final com.badlogic.gdx.scenes.scene2d.ui.Label nameText = new Label(
                "Statistics", graphics.getSkin(), "mainTitle");
        nameText.setAlignment(Align.center);
        nameText.setWrap(true);
        table.add(nameText)
                .width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() / 7f).row();

        plot = new Plot();

        table.add(plot)
                .width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() * 4 / 7f).row();

        stage.addActor(table);
    }

    @Override
    public void show() {
        CursorUtils.setDefaultCursor();
        Gdx.input.setInputProcessor(stage);
        wasSimulationPaused = Simulation.isPaused();
        simulation.setPaused(true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        simulation.setPaused(wasSimulationPaused);
    }

    @Override
    public void dispose() {
        super.dispose();
        hide();
        stage.dispose();
    }

    @Override
    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }
}
