package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.protoevo.core.Particle;
import com.protoevo.input.*;

import java.util.Collection;

public class SimulationInputManager {

    private final ParticleTracker particleTracker;
    private final MoveParticleButton moveParticleButton;
    private final LightningButton lightningButton;
    private final InputLayers inputLayers;


    public SimulationInputManager(SimulationScreen simulationScreen)  {
        OrthographicCamera camera = simulationScreen.getCamera();
        ToggleDebug toggleDebug = new ToggleDebug();

        inputLayers = new InputLayers(simulationScreen.getStage(), toggleDebug);
        Gdx.input.setInputProcessor(inputLayers);

        inputLayers.addLayer(new SimulationKeyboardControls(simulationScreen));

        Collection<? extends Particle> particles = simulationScreen.getEnvironment().getParticles();
        inputLayers.addLayer(new ApplyForcesInput(simulationScreen));
        PanZoomCameraInput panZoomCameraInput = new PanZoomCameraInput(camera);

        TopBar topBar = simulationScreen.getTopBar();

        lightningButton = new LightningButton(this, topBar.getButtonSize());
        inputLayers.addLayer(new LightningStrikeInput(simulationScreen, lightningButton));

        particleTracker = new ParticleTracker(particles, camera, panZoomCameraInput);

        moveParticleButton = new MoveParticleButton(topBar.getButtonSize());
        Vector2 pos = topBar.nextLeftButtonPosition();
        moveParticleButton.setPosition(pos.x, pos.y);

        SpawnParticleInput spawnParticleInput = new SpawnParticleInput(simulationScreen);
        MoveParticle moveParticle = new MoveParticle(simulationScreen, moveParticleButton, particleTracker);
        CursorUpdater cursorUpdater = new CursorUpdater(simulationScreen, this);

        ImageButton jediButton = simulationScreen.createBarImageButton("icons/jedi_on.png", event -> {
            if (event.toString().equals("touchDown")) {
                moveParticle.toggleJediMode();
                ImageButton button = (ImageButton) event.getListenerActor();

                Drawable tmp = button.getStyle().imageUp;
                button.getStyle().imageUp = button.getStyle().imageDown;
                button.getStyle().imageDown = tmp;
            }
            return true;
        });
        TextureRegion region = new TextureRegion(new Texture("icons/jedi_off.png"));
        jediButton.getStyle().imageDown = new TextureRegionDrawable(region);

        inputLayers.addLayers(cursorUpdater, spawnParticleInput, moveParticle, particleTracker);

        inputLayers.addLayer(panZoomCameraInput);


        topBar.addLeft(moveParticleButton);
        topBar.addLeft(jediButton);
        topBar.addLeft(lightningButton);
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
}
