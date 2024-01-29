package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.protoevo.ui.UIStyle;
import com.protoevo.ui.rendering.Renderer;
import com.protoevo.ui.shaders.ShaderLayers;

public class SimpleSDFDemoScreen extends ScreenAdapter {

    private final Renderer renderer;
    private final SpriteBatch uiBatch;
    private final BitmapFont debugFont;

    public SimpleSDFDemoScreen() {
        renderer = new ShaderLayers(
                new SDFDemoRenderer(),
                new SimpleSDFDemoShader()
        );
        uiBatch = new SpriteBatch();
        debugFont = UIStyle.createFiraCode(20);
    }

    @Override
    public void render(float delta) {
        renderer.render(delta);
        String debugString = "FPS: " + Gdx.graphics.getFramesPerSecond();
        uiBatch.begin();
        debugFont.setColor(Color.GOLD);
        float pad = debugFont.getLineHeight() * 0.5f;
        debugFont.draw(uiBatch, debugString, pad, Gdx.graphics.getHeight() - pad);
        uiBatch.end();
    }

    @Override
    public void show() {}

    @Override
    public void dispose() {
        renderer.dispose();
        uiBatch.dispose();
        debugFont.dispose();
    }
}
