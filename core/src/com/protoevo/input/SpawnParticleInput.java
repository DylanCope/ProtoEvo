package com.protoevo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.protoevo.biology.PlantCell;
import com.protoevo.core.Particle;
import com.protoevo.env.Environment;
import com.protoevo.utils.Geometry;

public class SpawnParticleInput extends InputAdapter {

    private final OrthographicCamera camera;
    private final Environment environment;

    private final float rate = 0.05f;
    private float timeSinceSpawn = 0;

    public SpawnParticleInput(OrthographicCamera camera, Environment environment) {
        this.camera = camera;
        this.environment = environment;
    }

    public void addParticle(float x, float y) {
        Particle particle = new PlantCell(environment);
        particle.setPos(new Vector2(x, y));
        Vector2 impulse = Geometry.fromAngle((float) (Math.random() * Math.PI * 2)).scl(.01f);
        particle.getBody().applyLinearImpulse(impulse, particle.getPos(), true);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        timeSinceSpawn = 0f;
        return false;
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
            timeSinceSpawn += Gdx.graphics.getDeltaTime();
            if (timeSinceSpawn > rate) {
                Vector3 worldSpace = camera.unproject(new Vector3(screenX, screenY, 0));
                addParticle(worldSpace.x, worldSpace.y);
                timeSinceSpawn = 0f;
            }
            return true;
        }
        return false;
    }

}
