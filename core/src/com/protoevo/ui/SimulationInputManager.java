package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.utils.Align;
import com.protoevo.biology.cells.MultiCellStructure;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.core.Simulation;
import com.protoevo.env.Serialization;
import com.protoevo.env.Spawnable;
import com.protoevo.ui.input.*;
import com.protoevo.ui.screens.SimulationScreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SimulationInputManager {
    private final SimulationScreen simulationScreen;
    private final TopBar topBar;
    private final ParticleTracker particleTracker;
    private final MoveParticleButton moveParticleButton;
    private final LightningButton lightningButton;
    private final InputLayers inputLayers;

    private final SelectBox<String> spawnableToSpawnSelectBox;
    private final Map<String, Supplier<Spawnable>> possibleSpawnables = new HashMap<>();


    public SimulationInputManager(SimulationScreen simulationScreen)  {
        this.simulationScreen = simulationScreen;
        OrthographicCamera camera = simulationScreen.getCamera();

        inputLayers = new InputLayers(simulationScreen.getStage(), new ToggleDebug());
        inputLayers.addLayer(new SimulationKeyboardControls(simulationScreen));
        inputLayers.addLayer(new ShockwaveForcesInput(simulationScreen));
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

        possibleSpawnables.put("Plant Cell", () -> Evolvable.createNew(PlantCell.class));
        possibleSpawnables.put("Random Protozoan", () -> Evolvable.createNew(Protozoan.class));
        tryLoadSavedProtozoans();

        spawnableToSpawnSelectBox = new SelectBox<>(graphics.getSkin());
        spawnableToSpawnSelectBox.setAlignment(Align.left);
        spawnableToSpawnSelectBox.setHeight(topBar.getButtonSize());
        simulationScreen.getLayout().setText(spawnableToSpawnSelectBox.getStyle().font, "Random Protozoa");
        spawnableToSpawnSelectBox.setWidth(simulationScreen.getLayout().width * 1.1f);
        refreshCellToAddSelectBox();
        topBar.addRight(spawnableToSpawnSelectBox);
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

        UserSpawnInput userSpawnInput = new UserSpawnInput(simulationScreen);
        MoveParticle moveParticle = new MoveParticle(simulationScreen, this, particleTracker);
        CursorUpdater cursorUpdater = new CursorUpdater(simulationScreen, this);

        inputLayers.addLayers(cursorUpdater, userSpawnInput, moveParticle, particleTracker);
        inputLayers.addLayer(panZoomCameraInput);

        topBar.addLeft(moveParticleButton);
        topBar.addLeft(lightningButton);
    }

    public InputLayers getInputLayers() {
        return inputLayers;
    }

    private Optional<Protozoan> tryLoadProtozoa(Path save) {
        try {
            return Optional.of(Serialization.deserialize(save.toString(), Protozoan.class));
        }
        catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<MultiCellStructure> tryLoadMulticell(Path save) {
        try {
            return Optional.of(Serialization.deserialize(save.toString(),
                                                     MultiCellStructure.class));
        }
        catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private void tryLoadSavedProtozoans() {
        try (Stream<Path> saved = Files.list(Paths.get("saved-cells"))) {
            saved.forEach(save -> {
                String filename = save.getFileName().toString();
                if (filename.endsWith(".cell")) {
                    String cellName = filename.replace(".cell", "");
                    tryLoadProtozoa(save).ifPresent(cell -> {
                        registerNewCloneableCell(cellName, cell);
                    });
                }
                else if (filename.endsWith(".multicell")) {
                    String cellName = filename.replace(".multicell", "");
                    tryLoadMulticell(save).ifPresent(multiCell -> {
                        registerNewSpawnable(
                                cellName,
                                () -> Serialization.clone(multiCell, MultiCellStructure.class));
                    });
                }
            });
        }
        catch (Exception ignored) {}
    }

    public void refreshCellToAddSelectBox() {
        spawnableToSpawnSelectBox.setItems(possibleSpawnables.keySet().toArray(new String[0]));
    }

    public void registerNewCloneableCell(String name, final Protozoan cell) {
        possibleSpawnables.put(name, () -> Evolvable.asexualClone(cell));
        refreshCellToAddSelectBox();
    }

    public void registerNewSpawnable(String name, Supplier<Spawnable> spawnable) {
        possibleSpawnables.put(name, spawnable);
        refreshCellToAddSelectBox();
    }

    public Spawnable createSelectedSpawnable() {
        return possibleSpawnables.get(spawnableToSpawnSelectBox.getSelected()).get();
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
