package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.core.Particle;
import com.protoevo.ui.SimulationScreen;

public class LightningStrikeInput extends InputAdapter {
    private final SimulationScreen simulationScreen;
    private final LightningButton lightningButton;


    public LightningStrikeInput(SimulationScreen simulationScreen, LightningButton lightningButton) {
        this.simulationScreen = simulationScreen;
        this.lightningButton = lightningButton;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && lightningButton.canStrike()) {
            Vector3 worldSpace = simulationScreen.getCamera().unproject(new Vector3(screenX, screenY, 0));
            for (Particle particle : simulationScreen.getEnvironment().getParticles()) {
                if (particle.getPos().dst(worldSpace.x, worldSpace.y) < particle.getRadius()) {
                    ParticleTracker particleTracker = simulationScreen.getInputManager().getParticleTracker();
                    if (particleTracker.isTracking() && particleTracker.getTrackedParticle() == particle) {
                        particleTracker.untrack();
                    }

                    particle.kill(CauseOfDeath.LIGHTNING_STRIKE);
                    return true;
                }
            }
        }
        return false;
    }
}
