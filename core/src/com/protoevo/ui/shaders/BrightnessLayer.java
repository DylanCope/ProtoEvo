package com.protoevo.ui.shaders;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class BrightnessLayer extends ShaderLayer {

    float brightness = 1f;

    public BrightnessLayer(OrthographicCamera camera) {
        super(camera, "darken");
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    @Override
    public void setShaderUniformVariables(ShaderProgram shaderProgram) {
        shaderProgram.setUniformf("u_brightness", brightness);
    }
}
