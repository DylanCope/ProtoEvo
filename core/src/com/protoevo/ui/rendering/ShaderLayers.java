package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.util.List;

public class ShaderLayers implements Renderer {

    private final List<ShaderLayer> layers;
    private final Renderer baseRenderer;
    private final SpriteBatch batch;
    private final FrameBuffer fbo;
    private final OrthographicCamera camera;

    public ShaderLayers(Renderer baseLayer, ShaderLayer... shaderLayers) {
        this.baseRenderer = baseLayer;
        layers = List.of(shaderLayers);
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

        fbo.bind();
        fbo.begin();
        baseRenderer.render(delta);
        fbo.end();

        float graphicsWidth = Gdx.graphics.getWidth();
        float graphicsHeight = Gdx.graphics.getHeight();

        for (int i = 0; i < layers.size(); i++) {
            ShaderLayer layer = layers.get(i);
            if (layer.isEnabled()) {
                ShaderProgram shaderProgram = layer.getShaderProgram();
                shaderProgram.bind();
                layer.setShaderUniformVariables(shaderProgram);

                batch.setShader(shaderProgram);
                Sprite sprite = getFBOSprite();
                if (i < layers.size() - 1) {
                    fbo.bind();
                    fbo.begin();
                    batch.begin();
                    batch.draw(sprite, 0, 0, graphicsWidth, graphicsHeight);
                    batch.end();
                    fbo.end();
                } else {
                    batch.begin();
                    batch.draw(sprite, 0, 0, graphicsWidth, graphicsHeight);
                    batch.end();
                }
            } else if (i == layers.size() - 1) {
                Sprite sprite = getFBOSprite();
                batch.begin();
                batch.draw(sprite, 0, 0, graphicsWidth, graphicsHeight);
                batch.end();
            }
            layer.update(delta);
        }
        batch.setShader(null);
    }

    @Override
    public void dispose() {
        baseRenderer.dispose();
        for (ShaderLayer layer : layers) {
            layer.dispose();
        }
    }
}
