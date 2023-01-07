package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.core.Particle;
import com.protoevo.ui.UI;
import com.protoevo.utils.CursorUtils;

import java.util.Collection;

public class LightningStrikeInput extends InputAdapter {
    private final UI ui;
    private final LightningButton lightningButton;


    public LightningStrikeInput(UI ui, LightningButton lightningButton) {
        this.ui = ui;
        this.lightningButton = lightningButton;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && lightningButton.canStrike()) {
            Vector3 worldSpace = ui.getCamera().unproject(new Vector3(screenX, screenY, 0));
            for (Particle particle : ui.getEnvironment().getParticles()) {
                if (particle.getPos().dst(worldSpace.x, worldSpace.y) < particle.getRadius()) {
                    ParticleTracker particleTracker = ui.getInputManager().getParticleTracker();
                    if (particleTracker.isTracking() && particleTracker.getTrackedParticle() == particle) {
                        particleTracker.untrack();
                    }

                    particle.kill();
                    return true;
                }
            }
        }
        return false;
    }
}
