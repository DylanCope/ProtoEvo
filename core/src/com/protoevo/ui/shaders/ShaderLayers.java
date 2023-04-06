package com.protoevo.ui.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.protoevo.ui.FrameBufferManager;
import com.protoevo.ui.rendering.Renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShaderLayers implements Renderer {

    private final List<ShaderLayer> layers;
    private final Renderer baseRenderer;
    private final SpriteBatch batch;
    private final FrameBuffer fbo;
    private final OrthographicCamera camera;
    private final FrameBufferManager fboManager;

    public ShaderLayers(Renderer baseLayer, ShaderLayer... shaderLayers) {
        this.baseRenderer = baseLayer;
        layers = Arrays.asList(shaderLayers);
        fboManager = FrameBufferManager.getInstance();

        if (!layers.isEmpty()) {
            camera = layers.get(0).getCamera();
            fbo = new FrameBuffer(Pixmap.Format.RGBA8888,
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
            batch = new SpriteBatch();
        } else {
            camera = null;
            fbo = null;
            batch = null;
        }
    }

    private Sprite getFBOSprite() {
        Sprite sprite = new Sprite(fbo.getColorBufferTexture());
        sprite.flip(false, true);
        return sprite;
    }

    @Override
    public void render(float delta) {
        if (layers.size() == 0) {
            baseRenderer.render(delta);
            return;
        }

        fboManager.begin(fbo);
        baseRenderer.render(delta);
        fboManager.end();

        for (int i = 0; i < layers.size(); i++) {
            ShaderLayer layer = layers.get(i);
            if (layer.isEnabled()) {
                ShaderProgram shaderProgram = layer.getShaderProgram();
                shaderProgram.bind();
                layer.setShaderUniformVariables(shaderProgram);

                batch.setShader(shaderProgram);
                Sprite sprite = getFBOSprite();
                if (i < layers.size() - 1) {
                    fboManager.begin(fbo);
                    draw(sprite);
                    fboManager.end();
                } else {
                    draw(sprite);
                }
            } else if (i == layers.size() - 1) {
                draw(getFBOSprite());
            }
            layer.update(delta);
        }
        batch.setShader(null);
    }

    private void draw(Sprite sprite) {
        float graphicsWidth = Gdx.graphics.getWidth();
        float graphicsHeight = Gdx.graphics.getHeight();
        batch.begin();
        batch.draw(sprite, 0, 0, graphicsWidth, graphicsHeight);
        batch.end();
    }

    @Override
    public void dispose() {
        baseRenderer.dispose();
        for (ShaderLayer layer : layers) {
            layer.dispose();
        }
    }
}
