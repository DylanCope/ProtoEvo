package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderLayer {

    public void setShaderUniformVariables(ShaderProgram shaderProgram) {}

    public boolean isEnabled() {
        return true;
    }

    private final ShaderProgram shaderProgram;
    private final SpriteBatch batch;
    private final OrthographicCamera camera;

    public ShaderLayer(OrthographicCamera camera, String shaderName) {
        this.camera = camera;
        batch = new SpriteBatch();
        shaderProgram = new ShaderProgram(
                Gdx.files.internal("shaders/" + shaderName + "/vertex.glsl").readString(),
                Gdx.files.internal("shaders/" + shaderName + "/fragment.glsl").readString());

        ShaderProgram.pedantic = false;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void update(float delta) {}

    public void dispose() {
        shaderProgram.dispose();
        batch.dispose();
    }
}
