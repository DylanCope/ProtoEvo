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
import com.protoevo.ui.plotting.PlotElement;
import com.protoevo.ui.plotting.PlotGrid;
import com.protoevo.ui.TopBar;
import com.protoevo.utils.CursorUtils;
import com.protoevo.utils.DebugMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class StatsGraphsScreen extends ScreenAdapter {

    private final Simulation simulation;
    private boolean wasSimulationPaused;
    private GraphicsAdapter graphics;
    private final Stage stage;
    private final PlotGrid plotGrid;
    private final com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String> xSelectBox, ySelectBox;
    private String selectedXAxis, selectedYAxis;
    private final LinePlot linePlot;

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

        final Table selectionTable = new Table();
        final com.badlogic.gdx.scenes.scene2d.ui.Label xSelectLabel = new Label(
                "X-Axis:", graphics.getSkin());
        final com.badlogic.gdx.scenes.scene2d.ui.Label ySelectLabel = new Label(
                "Y-Axis:", graphics.getSkin());
        xSelectBox = new com.badlogic.gdx.scenes.scene2d.ui.SelectBox<>(graphics.getSkin());
        xSelectBox.setWidth(1.2f * Gdx.graphics.getWidth() / 5f);

        ySelectBox = new com.badlogic.gdx.scenes.scene2d.ui.SelectBox<>(graphics.getSkin());
        ySelectBox.setWidth(1.2f * Gdx.graphics.getWidth() / 5f);
        selectionTable.add(xSelectLabel);
        selectionTable.add().width(Gdx.graphics.getWidth() / 50f);
        selectionTable.add(xSelectBox);
        selectionTable.add().width(Gdx.graphics.getWidth() / 30f);
        selectionTable.add(ySelectLabel);
        selectionTable.add().width(Gdx.graphics.getWidth() / 50f);
        selectionTable.add(ySelectBox);
        selectionTable.padBottom(Gdx.graphics.getWidth() / 30f);
        table.add(selectionTable).row();

        selectedXAxis = xSelectBox.getSelected();
        selectedYAxis = ySelectBox.getSelected();

        plotGrid = new PlotGrid(stage);
        plotGrid.setSize(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() * 4 / 7f);

        linePlot = new LinePlot();

        plotGrid.add(linePlot);

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

        simulation.makeStatisticsSnapshot();
        Collection<String> commonStatsKeys = simulation.getHistory().getCommonStatisticsKeys();
        commonStatsKeys = commonStatsKeys.stream()
                .filter(key -> {
                    return !key.contains("Node")
                            && !key.contains("Organelle");
                })
                .sorted()
                .collect(Collectors.toList());
        xSelectBox.setItems(commonStatsKeys.toArray(new String[0]));
        ySelectBox.setItems(commonStatsKeys.toArray(new String[0]));
        xSelectBox.setSelected("Time Elapsed");
        ySelectBox.setSelected("Protozoa");
        refreshPlot();
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

    private void refreshPlot() {
        ArrayList<Vector2> data = simulation.getHistory().extractData(
                xSelectBox.getSelected(), ySelectBox.getSelected());

        if (data.isEmpty()) {
            return;
        }


//        float xMin = 0, xMax = .5f, yMin = -1.5f, yMax = 1.5f;
//        int resolution = 1000;
//        ArrayList<Vector2> data = new ArrayList<>();
//        for (int i = 0; i < resolution; i++) {
//            float x = xMin + (xMax - xMin) * i / resolution;
//            float y = (float) Math.cos(x);
//            data.add(new Vector2(x, y));
//        }
//        xMin = Float.MAX_VALUE; xMax = -Float.MAX_VALUE; yMin = Float.MAX_VALUE; yMax = -Float.MAX_VALUE;

        linePlot.setData(data)
                .setLineWidth(4)
                .setLineColor(Color.RED);
        float xMin = Float.MAX_VALUE, xMax = -Float.MAX_VALUE, yMin = Float.MAX_VALUE, yMax = -Float.MAX_VALUE;
        for (Vector2 point : data) {
            xMin = Math.min(xMin, point.x);
            xMax = Math.max(xMax, point.x);
            yMin = Math.min(yMin, point.y);
            yMax = Math.max(yMax, point.y);
        }
        float padPercent = 0.1f;
        xMin -= Math.abs(xMax - xMin) * padPercent;
        xMax += Math.abs(xMax - xMin) * padPercent;
        yMin -= Math.abs(yMax - yMin) * padPercent;
        yMax += Math.abs(yMax - yMin) * padPercent;

        plotGrid.setPlotBounds(xMin, xMax, yMin, yMax);
        plotGrid.setMajorTicks(Math.abs(xMax - xMin) / 5f, Math.abs(yMax - yMin) / 5f);
        plotGrid.setMinorTicks(Math.abs(xMax - xMin) / 25f, Math.abs(yMax - yMin) / 25f);
    }

    @Override
    public void render(float delta) {
        stage.act(delta);

        if (!xSelectBox.getSelected().equals(selectedXAxis)
                || !ySelectBox.getSelected().equals(selectedYAxis)) {
            refreshPlot();
        }

        selectedXAxis = xSelectBox.getSelected();
        selectedYAxis = ySelectBox.getSelected();

        stage.draw();
    }
}
