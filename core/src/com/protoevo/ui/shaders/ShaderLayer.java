package com.protoevo.ui.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.protoevo.ui.UIStyle;

public class ShaderLayer {

    public void setShaderUniformVariables(ShaderProgram shaderProgram) {}

    public boolean isEnabled() {
        return true;
    }

    private final ShaderProgram shaderProgram;
    private final SpriteBatch batch;
    private final OrthographicCamera camera;

    public ShaderLayer(String shaderName) {
        this(null, shaderName);
    }

    public ShaderLayer(OrthographicCamera camera, String shaderName) {
        this.camera = camera;
        batch = new SpriteBatch();
        shaderProgram = new ShaderProgram(
                Gdx.files.internal("shaders/" + shaderName + "/vertex.glsl").readString(),
                Gdx.files.internal("shaders/" + shaderName + "/fragment.glsl").readString());

        if (!shaderProgram.isCompiled()) {
            throw new RuntimeException("Shader " + shaderName + " compilation failed: " + shaderProgram.getLog());
        }
        ShaderProgram.pedantic = false;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void render() {
        if (isEnabled()) {
            ShaderProgram shaderProgram = getShaderProgram();
            shaderProgram.bind();
            setShaderUniformVariables(shaderProgram);
            batch.setShader(shaderProgram);
            batch.begin();
            batch.end();
        }
    }

    public void update(float delta) {}

    public void dispose() {
        shaderProgram.dispose();
        batch.dispose();
    }
}
