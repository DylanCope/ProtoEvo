package com.protoevo.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.protoevo.core.Simulation;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.ui.plotting.LinePlot;
import com.protoevo.ui.plotting.PlotGrid;
import com.protoevo.ui.TopBar;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.DebugMode;

import java.util.ArrayList;

public class StatsGraphsScreen extends ScreenAdapter {

    private Simulation simulation;
    private boolean wasSimulationPaused;
    private GraphicsAdapter graphics;
    private final Stage stage;
    private final PlotGrid plotGrid;

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

        plotGrid = new PlotGrid(stage);
        plotGrid.setSize(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() * 4 / 7f);

        float xMin = -5f, xMax = 5f, yMin = -1.5f, yMax = 1.5f;
        int resolution = 1000;
        ArrayList<Vector2> data = new ArrayList<>();
        for (int i = 0; i < resolution; i++) {
            float x = xMin + (xMax - xMin) * i / resolution;
            float y = (float) Math.sin(x);
            data.add(new Vector2(x, y));
        }
        LinePlot linePlot = new LinePlot()
                .setData(data)
                .setLineWidth(4)
                .setLineColor(Color.RED);

        plotGrid.add(linePlot);
        plotGrid.setPlotBounds(xMin, xMax, yMin, yMax);

        table.add(plotGrid).width(plotGrid.getPixelWidth()).height(plotGrid.getPixelHeight()).row();

        stage.addActor(table);

        TopBar topBar = new TopBar(
                this.stage,
                graphics.getSkin().getFont("default").getLineHeight()
        );
        topBar.createRightBarImageButton("icons/back.png", graphics::moveToPreviousScreen);
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
