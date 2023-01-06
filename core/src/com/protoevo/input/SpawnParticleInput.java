package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.core.Particle;
import com.protoevo.env.Environment;
import com.protoevo.utils.Geometry;

import java.util.Collection;

public class SpawnParticleInput extends InputAdapter {

    private final OrthographicCamera camera;
    private final Collection<Particle> particles;
    private final Environment environment;

    public SpawnParticleInput(OrthographicCamera camera, Collection<Particle> particles, Environment environment) {
        this.camera = camera;
        this.particles = particles;
        this.environment = environment;
    }

    public void addParticle(float x, float y) {
        Particle particle = new Particle();
        particle.setEnv(environment);
        float r = 3 + (int) (Math.random() * 8);
        particle.setPos(new Vector2(x, y));
        particle.setRadius(r);

        Vector2 impulse = Geometry.fromAngle((float) (Math.random() * Math.PI * 2)).scl(100f);
        particle.getBody().applyLinearImpulse(impulse, particle.getPos(), true);

        particles.add(particle);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
            addParticle(worldSpace.x, worldSpace.y);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
            addParticle(worldSpace.x, worldSpace.y);
            return true;
        }
        return false;
    }

}
