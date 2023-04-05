package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.env.LightMap;
import com.protoevo.utils.DebugMode;

public class LightRenderer implements Renderer {
    private final Environment environment;
    private final LightMap lightMap;
    private final SpriteBatch batch;
    private final ShaderProgram shader;
    private final Texture lightTexture;
    private final OrthographicCamera camera;

    public LightRenderer(OrthographicCamera camera, Environment environment) {
        this.environment = environment;
        this.camera = camera;

        lightMap = environment.getLightMap();
        int w = lightMap.getWidth();
        int h = lightMap.getHeight();

        Pixmap lightPixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        Color color = new Color();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                float light = lightMap.getLight(x, y);
                color.set(0.05f, 0f, 0.06f, 1 - light);
                lightPixmap.drawPixel(x, h - y, Color.rgba8888(color));
            }
        }

        lightTexture = new Texture(lightPixmap);
        lightPixmap.dispose();

        batch = new SpriteBatch();
        shader = new ShaderProgram(
                Gdx.files.internal("shaders/chemical/vertex.glsl"),
                Gdx.files.internal("shaders/chemical/fragment.glsl"));
        if (!shader.isCompiled()) {
            throw new RuntimeException("Shader compilation failed: " + shader.getLog());
        }
    }

    public void render(float delta) {
        batch.enableBlending();
        batch.setProjectionMatrix(camera.combined);
        if (!DebugMode.isModeOrHigher(DebugMode.INTERACTION_INFO)) {
            shader.bind();
            shader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shader.setUniformf("u_blurAmount", 4f);
            batch.setShader(shader);
        } else {
            batch.setShader(null);
        }

        batch.begin();
        batch.setColor(1, 1, 1, 0.7f);
        batch.draw(lightTexture, lightMap.getXMin(), lightMap.getYMin(),
                lightMap.getFieldWidth(), lightMap.getFieldWidth());
        batch.end();
    }

    @Override
    public void dispose() {
        lightTexture.dispose();
        batch.dispose();
        shader.dispose();
    }
}
