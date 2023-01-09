package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public abstract class ShaderLayer implements Renderer {
    public abstract Renderer getLayerBelow();
    public abstract ShaderProgram getShaderProgram();
    public abstract void setShaderUniformVariables();
    public abstract boolean isEnabled();

    private final SpriteBatch batch;
    private final FrameBuffer fbo;
    private final OrthographicCamera camera;

    public ShaderLayer(OrthographicCamera camera) {
        this.camera = camera;
        batch = new SpriteBatch();

        fbo = new FrameBuffer(
                Pixmap.Format.RGBA8888,
                (int) camera.viewportWidth,
                (int) camera.viewportHeight,
                false);
    }

    public void render() {
        if (isEnabled()) {
            fbo.begin();
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
            getLayerBelow().render();
            fbo.end();

            Sprite sprite = new Sprite(fbo.getColorBufferTexture());
            sprite.flip(false, true);

            ShaderProgram shaderProgram = getShaderProgram();
            shaderProgram.bind();
            setShaderUniformVariables();
            batch.setShader(shaderProgram);
            batch.begin();
            batch.draw(sprite, 0, 0, camera.viewportWidth, camera.viewportHeight);
            batch.end();
            batch.setShader(null);
        } else {
            getLayerBelow().render();
        }
    }
}
