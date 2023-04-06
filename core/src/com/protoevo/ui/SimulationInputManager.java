package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.utils.Align;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.core.Simulation;
import com.protoevo.env.EnvFileIO;
import com.protoevo.input.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SimulationInputManager {
    private final SimulationScreen simulationScreen;
    private final TopBar topBar;
    private final ParticleTracker particleTracker;
    private final MoveParticleButton moveParticleButton;
    private final LightningButton lightningButton;
    private final InputLayers inputLayers;

    private final SelectBox<String> cellToAddSelectBox;
    private final Map<String, Supplier<Cell>> possibleCellsToAdd = new HashMap<>();


    public SimulationInputManager(SimulationScreen simulationScreen)  {
        this.simulationScreen = simulationScreen;
        OrthographicCamera camera = simulationScreen.getCamera();

        inputLayers = new InputLayers(simulationScreen.getStage(), new ToggleDebug());
        inputLayers.addLayer(new SimulationKeyboardControls(simulationScreen));
        inputLayers.addLayer(new ApplyForcesInput(simulationScreen));
        PanZoomCameraInput panZoomCameraInput = new PanZoomCameraInput(camera);
        panZoomCameraInput.setOnPanOrZoomCallback(simulationScreen::disableMeandering);

        topBar = simulationScreen.getTopBar();
        Simulation simulation = simulationScreen.getSimulation();
        GraphicsAdapter graphics = simulationScreen.getGraphics();

        // Right side ui

        topBar.createRightBarImageButton("icons/x-button.png", () -> {
            simulation.onOtherThread(simulation::close);
            simulationScreen.addConditionalTask(
                    () -> !simulation.isBusyOnOtherThread(),
                    graphics::exitApplication
            );
        });

        topBar.createRightBarImageButton("icons/gear.png", simulationScreen::moveToPauseScreen);
        topBar.createRightBarImageButton("icons/save.png", simulation::saveOnOtherThread);

        possibleCellsToAdd.put("Plant Cell", () -> Evolvable.createNew(PlantCell.class));
        possibleCellsToAdd.put("Random Protozoan", () -> Evolvable.createNew(Protozoan.class));
        tryLoadSavedProtozoans();

        cellToAddSelectBox = new SelectBox<>(graphics.getSkin());
        cellToAddSelectBox.setAlignment(Align.left);
        cellToAddSelectBox.setHeight(topBar.getButtonSize());
        simulationScreen.getLayout().setText(cellToAddSelectBox.getStyle().font, "Random Protozoa");
        cellToAddSelectBox.setWidth(simulationScreen.getLayout().width * 1.1f);
        refreshCellToAddSelectBox();
        topBar.addRight(cellToAddSelectBox);
        Label label = new Label("Add Cell: ", graphics.getSkin());
        label.setHeight(topBar.getButtonSize());
        label.setAlignment(Align.right);
        topBar.addRight(label);

        // Left side ui

        topBar.createLeftBarImageButton("icons/play_pause.png", simulation::togglePause);
        topBar.createLeftBarImageButton("icons/fast_forward.png", simulation::toggleTimeDilation);
        topBar.createLeftBarImageButton("icons/home_icon.png", simulationScreen::resetCamera);
        topBar.createLeftBarImageButton("icons/folder.png", simulation::openSaveFolderOnDesktop);
        topBar.createLeftBarImageButton("icons/terminal.png", graphics::switchToHeadlessMode);

        topBar.createLeftBarToggleImageButton(
                "icons/meander_disabled.png", "icons/meander_enabled.png",
                simulationScreen::toggleMeandering
        );

        // Input layers

        lightningButton = new LightningButton(this, topBar.getButtonSize());
        inputLayers.addLayer(new LightningStrikeInput(simulationScreen, lightningButton));
        particleTracker = new ParticleTracker(simulationScreen, panZoomCameraInput);

        moveParticleButton = new MoveParticleButton(topBar.getButtonSize());
        Vector2 pos = topBar.nextLeftPosition();
        moveParticleButton.setPosition(pos.x, pos.y);

        SpawnParticleInput spawnParticleInput = new SpawnParticleInput(simulationScreen);
        MoveParticle moveParticle = new MoveParticle(simulationScreen, this, particleTracker);
        CursorUpdater cursorUpdater = new CursorUpdater(simulationScreen, this);

        inputLayers.addLayers(cursorUpdater, spawnParticleInput, moveParticle, particleTracker);
        inputLayers.addLayer(panZoomCameraInput);

        topBar.addLeft(moveParticleButton);
        topBar.addLeft(lightningButton);
    }

    private Cell tryLoad(Path save) {
        try {
            return EnvFileIO.deserialize(save.toString(), Protozoan.class);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private void tryLoadSavedProtozoans() {
        try (Stream<Path> saved = Files.list(Paths.get("saved-cells"))) {
            saved.forEach(save -> {
                String name = save.getFileName().toString().replace(".cell", "");
                Cell cell = tryLoad(save);
                if (cell instanceof Protozoan)
                    registerNewCloneableCell(name, (Protozoan) cell);
            });
        }
        catch (Exception ignored) {}
    }

    public void refreshCellToAddSelectBox() {
        cellToAddSelectBox.setItems(possibleCellsToAdd.keySet().toArray(new String[0]));
    }

    public void registerNewCloneableCell(String name, final Protozoan protozoan) {
        possibleCellsToAdd.put(
                name,
                () -> Evolvable.asexualClone(protozoan));
        refreshCellToAddSelectBox();
    }

    public Cell createNewCell() {
        return possibleCellsToAdd.get(cellToAddSelectBox.getSelected()).get();
    }

    public void registerAsInputProcessor() {
        Gdx.input.setInputProcessor(inputLayers);
    }

    public ParticleTracker getParticleTracker() {
        return particleTracker;
    }

    public MoveParticleButton getMoveParticleButton() {
        return moveParticleButton;
    }

    public LightningButton getLightningButton() {
        return lightningButton;
    }

    public Vector2 getMousePos() {
        return inputLayers.getMousePos();
    }

    public void dispose() {
        MoveParticleButton.dispose();
        LightningButton.dispose();
    }
}
