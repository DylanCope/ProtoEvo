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
import com.protoevo.ui.rendering.Renderer;

import java.util.Collection;

public class InputManager {

    private final ToggleDebug toggleDebug;
    private final ParticleTracker particleTracker;

    public InputManager(UI ui)  {
        OrthographicCamera camera = ui.getCamera();
        toggleDebug = new ToggleDebug();

        InputLayers inputLayers = new InputLayers(ui.getStage(), toggleDebug);
        Gdx.input.setInputProcessor(inputLayers);

        Collection<Particle> particles = ui.getEnvironment().getParticles();
        inputLayers.addLayer(new ApplyForcesInput(particles, camera));
        PanZoomCameraInput panZoomCameraInput = new PanZoomCameraInput(camera);

        TopBar topBar = ui.getTopBar();

        particleTracker = new ParticleTracker(particles, camera, panZoomCameraInput);

        MoveParticleButton moveParticleButton = new MoveParticleButton(topBar.getButtonSize());
        Vector2 pos = topBar.nextLeftButtonPosition();
        moveParticleButton.setPosition(pos.x, pos.y);
        topBar.addLeft(moveParticleButton);

        SpawnParticleInput spawnParticleInput = new SpawnParticleInput(camera, particles, ui.getEnvironment());
        MoveParticle moveParticle = new MoveParticle(camera, particles, moveParticleButton, particleTracker);
        CursorUpdater cursorUpdater = new CursorUpdater(camera, particles, moveParticleButton, particleTracker);

        ImageButton jediButton = ui.createBarImageButton("icons/jedi_off.png", event -> {
            if (event.toString().equals("touchDown")) {
                moveParticle.toggleJediMode();
                ImageButton button = (ImageButton) event.getListenerActor();

                Drawable tmp = button.getStyle().imageUp;
                button.getStyle().imageUp = button.getStyle().imageDown;
                button.getStyle().imageDown = tmp;
            }
            return true;
        });
        TextureRegion region = new TextureRegion(new Texture("icons/jedi_on.png"));
        jediButton.getStyle().imageDown = new TextureRegionDrawable(region);
        topBar.addLeft(jediButton);

        inputLayers.addLayers(cursorUpdater, spawnParticleInput, moveParticle, particleTracker);
        inputLayers.addLayer(panZoomCameraInput);
    }

    public ParticleTracker getParticleTracker() {
        return particleTracker;
    }

    public boolean isDebugActivated() {
        return toggleDebug.isDebug();
    }
}
