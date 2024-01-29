package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.ScreenAdapter;
import com.protoevo.ui.shaders.ShaderLayer;
import com.protoevo.ui.shaders.ShaderLayers;

public class SDFDemoScreen extends ScreenAdapter {

    private ShaderLayer shader;

    public SDFDemoScreen() {
        shader = new SDFDemoShader();
    }

    @Override
    public void render(float delta) {
        shader.render();
    }

    @Override
    public void show() {

    }

    @Override
    public void dispose() {

    }
}
