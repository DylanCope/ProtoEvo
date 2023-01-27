package com.protoevo.ui.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class BlurLayer extends ShaderLayer {

    public BlurLayer(OrthographicCamera camera) {
        super(camera, "blur");
    }

    @Override
    public void setShaderUniformVariables(ShaderProgram shaderProgram) {
        float graphicsWidth = Gdx.graphics.getWidth();
        float graphicsHeight = Gdx.graphics.getHeight();
        shaderProgram.setUniformf("u_resolution", graphicsWidth, graphicsHeight);
    }
}
