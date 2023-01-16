package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.core.Particle;
import com.protoevo.ui.shaders.ShockWaveLayer;

import java.util.Collection;

public class ApplyForcesInput extends InputAdapter {

    Collection<? extends Particle> entities;
    OrthographicCamera camera;

    public ApplyForcesInput(Collection<? extends Particle> entities, OrthographicCamera camera) {
        this.entities = entities;
        this.camera = camera;
    }

    public void applyForce(float explosionX, float explosionY, float power) {
        if (ShockWaveLayer.getInstance() != null)
            ShockWaveLayer.getInstance().start(explosionX, explosionY);
        Vector2 tmp = new Vector2();
        for (Particle particle : entities) {
            Vector2 bodyPos = particle.getPos();
            tmp.set(bodyPos.x - explosionX, bodyPos.y - explosionY);
            float dist2 = tmp.len2();
            if (power / dist2 > 1) {
                float explosionFallout = 10f;
                tmp.setLength((float) (power * Math.exp(-explosionFallout * dist2)));
                particle.getBody().applyLinearImpulse(tmp,  bodyPos, true);
            }
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
            applyForce(worldSpace.x, worldSpace.y, .1f);
        }
        return false;
    }
}
