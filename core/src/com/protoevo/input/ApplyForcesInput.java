package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.protoevo.core.Particle;

import java.util.Collection;

public class ApplyForcesInput extends InputAdapter {

    Collection<Particle> particles;
    OrthographicCamera camera;

    public ApplyForcesInput(Collection<Particle> particles, OrthographicCamera camera) {
        this.particles = particles;
        this.camera = camera;
    }

    public void applyForce(float explosionX, float explosionY, float power) {
        Vector2 tmp = new Vector2();
        for (Particle particle : particles) {
            Vector2 bodyPos = particle.getPos();
            tmp.set(bodyPos.x - explosionX, bodyPos.y - explosionY);
            tmp.setLength(power / tmp.len2());
            particle.getBody().applyLinearImpulse(tmp,  bodyPos, true);
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
            applyForce(worldSpace.x, worldSpace.y, 1000000);
        }
        return false;
    }
}
