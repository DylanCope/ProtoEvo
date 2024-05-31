package com.protoevo.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.protoevo.settings.Parameter;
import com.protoevo.settings.Settings;
import com.protoevo.ui.GraphicsAdapter;
import com.protoevo.ui.TopBar;
import com.protoevo.utils.DebugMode;

import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class EditSettingsScreen extends ScreenAdapter {
    private final Stage stage;
    private final GraphicsAdapter graphics;
    private final String settingsName;
    private final Skin skin;
    private Consumer<Float> drawBackground;
    private final ScreenAdapter previousScreen;

    public EditSettingsScreen(
            ScreenAdapter previousScreen,
            GraphicsAdapter graphics,
            Settings settings,
            List<Settings> otherSettings
    ) {
        this(previousScreen, graphics, settings, otherSettings, () -> {});
    }

    public EditSettingsScreen(
            ScreenAdapter previousScreen,
            GraphicsAdapter graphics,
            Settings settings,
            List<Settings> otherSettings,
            Runnable onApply
    ) {
        this.graphics = graphics;
        this.settingsName = settings.getName();
        this.previousScreen = previousScreen;

        this.stage = new Stage();
        stage.setDebugAll(DebugMode.isDebugMode());

        TopBar topBar = new TopBar(this.stage, graphics.getSkin().getFont("default").getLineHeight());

        topBar.createRightBarImageButton("icons/back.png", this::returnToPreviousScreen);

        skin = graphics.getSkin();
        final Table scrollTable = new Table();

        final Map<Parameter<?>, Widget> parameterFields = new HashMap<>();

        for (Parameter<?> parameter : settings.getParameters()) {
            if (parameter.getName() == null || parameter.getName().equals(""))
                continue;

            Widget field = createParameterInput(parameter, scrollTable);
            parameterFields.put(parameter, field);
        }

        final ScrollPane scroller = new ScrollPane(scrollTable);
        scroller.setScrollbarsVisible(true);
        scroller.setScrollingDisabled(true, false);
        
        final Table table = new Table();

        final Label nameText = new Label(settingsName + " Settings", skin, "mediumTitle");
        nameText.setAlignment(Align.center);
        nameText.setWrap(true);

        table.setFillParent(true);
        table.add(nameText).width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() / 7f).row();

        if (otherSettings != null && otherSettings.size() > 0) {
            Table settingsOptionsTable = new Table();
            float height = 0;
            for (Settings option : otherSettings) {
                if (option == settings)
                    continue;
                final TextButton goToSettings = new TextButton(option.getName(), skin);
                goToSettings.addListener(e -> {
                    if (e.toString().equals("touchDown")) {
                        graphics.setScreen(new EditSettingsScreen(
                                previousScreen, graphics, option, otherSettings
                        ));
                    }
                    return true;
                });
                goToSettings.pad(goToSettings.getHeight() / 2f);
                height = Math.max(height, goToSettings.getHeight());
                settingsOptionsTable.add(goToSettings);
            }
            ScrollPane optionsScroller = new ScrollPane(settingsOptionsTable);
            optionsScroller.setScrollbarsVisible(true);
            optionsScroller.setScrollingDisabled(false, true);
            table.add(optionsScroller).width(4 * Gdx.graphics.getWidth() / 5f).height(4*height).row();
        }

        table.add(scroller).width(Gdx.graphics.getWidth() / 2f).height(Gdx.graphics.getHeight() * 4 / 7f).row();

        final TextButton applyButton = new TextButton("Apply", skin);
        applyButton.addListener(e -> {
            if (e.toString().equals("touchDown")) {
                for (Map.Entry<Parameter<?>, Widget> entry : parameterFields.entrySet()) {
                    Parameter<?> parameter = entry.getKey();
                    Widget widget = entry.getValue();
                    String enteredValue = getEnteredValue(widget);
                    if (enteredValue.equals(parameter.get().toString())) {
                        continue;
                    }
                    try {
                        parameter.set(enteredValue);
                    } catch (RuntimeException ex) {
                        System.err.println(
                            "Failed to set parameter " + parameter.getName() + " to " + enteredValue);
                    }
                }
                onApply.run();
            }
            return true;
        });
        applyButton.pad(applyButton.getHeight() / 2f);

        table.add(applyButton).padTop(applyButton.getHeight()).width(applyButton.getWidth() * 1.2f);

        this.stage.addActor(table);
    }

    public String getEnteredValue(Widget widget) {
        if (widget instanceof TextField) {
            return ((TextField) widget).getText();
        } else if (widget instanceof SelectBox) {
            return ((SelectBox<?>) widget).getSelected().toString();
        }
        throw new RuntimeException("Unknown widget type: " + widget.getClass());
    }

    private void returnToPreviousScreen() {
        graphics.setScreen(previousScreen);
        if (previousScreen instanceof SimulationScreen) {
            ((SimulationScreen) previousScreen).getSimulation().setPaused(false);
        } else if (previousScreen instanceof PauseScreen) {
            PauseScreen pauseScreen = (PauseScreen) previousScreen;
            pauseScreen.setTimePaused(pauseScreen.getFadeTime());
        }
    }

    private Widget createParameterInput(Parameter<?> parameter, Table table) {
        float scrollWidth = Gdx.graphics.getWidth() / 2f;
        final Label label = new Label(parameter.getName() + ": ", skin);

        Widget inputWidget;

        if (parameter.hasOptions()) {
            SelectBox<String> selectBox = new SelectBox<>(skin);
            Object[] options = parameter.getOptions();
            String[] optionsStrings = new String[options.length];
            for (int i = 0; i < options.length; i++) {
                optionsStrings[i] = options[i].toString();
            }
            selectBox.setItems(optionsStrings);
            selectBox.setSelected(parameter.get().toString());
            inputWidget = selectBox;
        }
        else {
            inputWidget = new TextField(parameter.get().toString(), skin);
        }

        inputWidget.setWidth(scrollWidth / 3f);
        final Table inputTable = new Table();

        inputTable.add(label)
                .width(scrollWidth * 2 / 3f)
                .align(Align.right);
        inputTable.add(inputWidget)
                .width(scrollWidth / 3f)
                .align(Align.left)
                .row();

        table.add(inputTable)
                .padBottom(label.getHeight() / 2f).row();

        return inputWidget;
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

        if (drawBackground != null)
            drawBackground.accept(delta);
        else
            GraphicsAdapter.renderBackground(delta);

        this.stage.draw();
    }

    @Override
    public void resize(final int width, final int height) {
        this.stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {}

    public void setBackgroundRenderer(Consumer<Float> drawBackground) {
        this.drawBackground = drawBackground;
    }
}
