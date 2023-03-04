package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.protoevo.core.Simulation;
import com.protoevo.core.Statistics;
import com.protoevo.ui.rendering.EnvironmentRenderer;
import com.protoevo.utils.FileIO;
import scala.Int;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LoadSaveScreen extends ScreenAdapter {
    private final Stage stage;
    private final GraphicsAdapter graphics;
    private final String simulationName;
    private final Skin skin;

    public LoadSaveScreen(GraphicsAdapter graphics, String simulationName) {
        this.graphics = graphics;
        this.simulationName = simulationName;

        this.stage = new Stage();

        TopBar topBar = new TopBar(this.stage, graphics.getSkin().getFont("default").getLineHeight());

        topBar.createRightBarImageButton("icons/x-button.png", graphics::exitApplication);

        topBar.createRightBarImageButton("icons/back.png", () -> {
            graphics.moveToTitleScreen(this);
        });

        skin = graphics.getSkin();
        final Table scrollTable = new Table();

        Simulation.getSavePaths(simulationName)
                .sorted(Comparator.comparing(Simulation::saveModifiedTime).reversed())
                .forEach(p -> addLoadScrollItem(p, scrollTable));

        final ScrollPane scroller = new ScrollPane(scrollTable);
        scroller.setScrollbarsVisible(true);
        scroller.setScrollbarsVisible(true);

        final Table table = new Table();

        final Label nameText = new Label(simulationName, skin, "mainTitle");
        nameText.setAlignment(Align.center);
        nameText.setWrap(true);

        table.setFillParent(true);
        table.add(nameText).width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() / 5f).row();
        table.add(scroller).width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() * 2 / 3f);

        this.stage.addActor(table);
    }

    private String reformatTimeStampString(String timeStamp) {
        // time stamp of form year-month-day-hour-minute-second
        // want to reformat to day/month/year hour:minute:second

        String[] splitTimeStamp = timeStamp.split("-");
        int year = Integer.parseInt(splitTimeStamp[0]);
        int month = Integer.parseInt(splitTimeStamp[1]);
        int day = Integer.parseInt(splitTimeStamp[2]);
        int hour = Integer.parseInt(splitTimeStamp[3]);
        int minute = Integer.parseInt(splitTimeStamp[4]);
        int second = Integer.parseInt(splitTimeStamp[5]);

        return String.format("%02d/%02d/%d %02d:%02d:%02d", day, month, year, hour, minute, second);
    }

    private Map<String, String> getStatsMap(Path savePath) {
        Map<String, String> statsMap = new HashMap<>();
        String saveTimeStamp = savePath.getFileName().toString();
        String timeStampPretty = reformatTimeStampString(saveTimeStamp);
        statsMap.put("Time Stamp", timeStampPretty);

        Long saveTime = Simulation.saveModifiedTime(savePath);
        Optional<Path> statsPath = Simulation.getClosestStatsPath(simulationName, saveTime);
        if (statsPath.isPresent()) {
            try {
                JsonNode stats = FileIO.readJson(statsPath.get().toString()).get("stats");
                statsMap.put("Time Elapsed", stats.get("Time Elapsed").get("value").asText());
                statsMap.put("Generation", stats.get("Max Protozoa Generation").get("value").asText());
                statsMap.put("Number of Protozoa", stats.get("Protozoa").get("value").asInt() + "");
            } catch (IOException e) {
                return statsMap;
            }
        }

        return statsMap;
    }

    private void addLoadScrollItem(Path path, Table scrollTable) {
        float scrollWidth = Gdx.graphics.getWidth() / 2f;

        Table statsTable = new Table();

        for (Map.Entry<String, String> entry : getStatsMap(path).entrySet()) {
            Label nameLabel = new Label(entry.getKey() + ": ", skin);
            nameLabel.setAlignment(Align.right);
            statsTable.add(nameLabel).width(scrollWidth / 2f);

            final Label text = new Label(entry.getValue(), skin);
            text.setAlignment(Align.left);
            text.setWrap(true);
            statsTable.add(text).width(scrollWidth / 2f);

            statsTable.row();
        }

        final String saveTimeStamp = path.getFileName().toString();
        TextButton loadButton = new TextButton("Load", skin);
        loadButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                graphics.loadSimulation(new Simulation(simulationName, saveTimeStamp));
            }
        });
        loadButton.align(Align.right).pad(loadButton.getHeight() / 5f);

        scrollTable.add(statsTable).width(scrollWidth).padBottom(loadButton.getHeight() / 2f);
        scrollTable.row();

        scrollTable.add(loadButton).padBottom(loadButton.getHeight());
        scrollTable.row();
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