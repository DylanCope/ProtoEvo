package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.protoevo.core.Simulation;
import com.protoevo.input.*;
import com.protoevo.settings.WorldGenerationSettings;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SimulationInputManager {
    private final SimulationScreen simulationScreen;
    private final TopBar topBar;
    private final ParticleTracker particleTracker;
    private final MoveParticleButton moveParticleButton;
    private final LightningButton lightningButton;
    private final InputLayers inputLayers;


    public SimulationInputManager(SimulationScreen simulationScreen)  {
        this.simulationScreen = simulationScreen;
        OrthographicCamera camera = simulationScreen.getCamera();

        inputLayers = new InputLayers(simulationScreen.getStage(), new ToggleDebug());
        inputLayers.addLayer(new SimulationKeyboardControls(simulationScreen));
        inputLayers.addLayer(new ApplyForcesInput(simulationScreen));
        PanZoomCameraInput panZoomCameraInput = new PanZoomCameraInput(camera);

        topBar = simulationScreen.getTopBar();
        Simulation simulation = simulationScreen.getSimulation();
        GraphicsAdapter graphics = simulationScreen.getGraphics();

        ImageButton closeButton = createBarImageButton("icons/x-button.png", event -> {
            graphics.exitApplication();
            return true;
        });
        topBar.addRight(closeButton);

        ImageButton backButton = createBarImageButton("icons/back.png", event -> {
            graphics.moveToTitleScreen(simulationScreen);
            simulation.save();
            return true;
        });
        topBar.addRight(backButton);

        ImageButton pauseButton = createBarImageButton("icons/play_pause.png", event -> {
            simulation.togglePause();
            return true;
        });
        topBar.addLeft(pauseButton);

        ImageButton toggleRenderingButton = createBarImageButton("icons/fast_forward.png", event -> {
            simulation.toggleTimeDilation();
            return true;
        });
        topBar.addLeft(toggleRenderingButton);

        ImageButton homeButton = createBarImageButton("icons/home_icon.png", event -> {
            camera.position.set(0, 0, 0);
            camera.zoom = WorldGenerationSettings.environmentRadius;
            return true;
        });
        topBar.addLeft(homeButton);

        ImageButton folderButton = createBarImageButton("icons/folder.png", event -> {
            try {
                Desktop.getDesktop().open(new File(simulation.getSaveFolder()));
            } catch (IOException e) {
                System.out.println("\nFailed to open folder: " + e.getMessage() + "\n");
            }
            return true;
        });
        topBar.addLeft(folderButton);

        ImageButton replButton = createBarImageButton("icons/terminal.png", event -> {
            graphics.switchToHeadlessMode();
            return true;
        });
        topBar.addLeft(replButton);

        lightningButton = new LightningButton(this, topBar.getButtonSize());
        inputLayers.addLayer(new LightningStrikeInput(simulationScreen, lightningButton));

        particleTracker = new ParticleTracker(simulationScreen, panZoomCameraInput);

        moveParticleButton = new MoveParticleButton(topBar.getButtonSize());
        Vector2 pos = topBar.nextLeftPosition();
        moveParticleButton.setPosition(pos.x, pos.y);

        SpawnParticleInput spawnParticleInput = new SpawnParticleInput(simulationScreen);
        MoveParticle moveParticle = new MoveParticle(simulationScreen, this, particleTracker);
        CursorUpdater cursorUpdater = new CursorUpdater(simulationScreen, this);

//        ImageButton jediButton = simulationScreen.createBarImageButton("icons/jedi_on.png", event -> {
//            moveParticle.toggleJediMode();
//            ImageButton button = (ImageButton) event.getListenerActor();
//            Drawable tmp = button.getStyle().imageUp;
//            button.getStyle().imageUp = button.getStyle().imageDown;
//            button.getStyle().imageDown = tmp;
//            return true;
//        });
//        TextureRegion region = new TextureRegion(ImageUtils.getTexture("icons/jedi_off.png"));
//        jediButton.getStyle().imageDown = new TextureRegionDrawable(region);

        inputLayers.addLayers(cursorUpdater, spawnParticleInput, moveParticle, particleTracker);
        inputLayers.addLayer(panZoomCameraInput);

        topBar.addLeft(moveParticleButton);
//        topBar.addLeft(jediButton);
        topBar.addLeft(lightningButton);
    }

    public ImageButton createBarImageButton(String texturePath, EventListener touchListener) {
        return simulationScreen.createImageButton(texturePath, topBar.getButtonSize(), topBar.getButtonSize(), event -> {
            if (event.toString().equals("touchDown")) {
                touchListener.handle(event);
            }
            return true;
        });
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
