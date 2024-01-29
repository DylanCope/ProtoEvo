package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.ui.shaders.ShaderLayer;

public class SDFDemoShader extends ShaderLayer {
    public SDFDemoShader() {
        super("sdfdemo");
    }

    @Override
    public void setShaderUniformVariables(ShaderProgram shaderProgram) {
        float graphicsWidth = Gdx.graphics.getWidth();
        float graphicsHeight = Gdx.graphics.getHeight();
        shaderProgram.setUniformf("resolution", new Vector2(graphicsWidth, graphicsHeight));
        shaderProgram.setUniformf("u_resolution", graphicsWidth, graphicsHeight);
    }
}
