package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.utils.DebugMode;

public class ChemicalsRenderer implements Renderer {
    private Environment environment;
    private final ChemicalSolution chemicalSolution;
    private final SpriteBatch batch;
    private final ShaderProgram shader;
    private final Texture chemicalTexture;
    private final Pixmap chemicalPixmap;
    private final OrthographicCamera camera;

    public ChemicalsRenderer(OrthographicCamera camera, Environment environment) {
        this.environment = environment;
        this.camera = camera;

        chemicalSolution = environment.getChemicalSolution();
        int w = chemicalSolution.getNXCells();
        int h = chemicalSolution.getNYCells();

        chemicalPixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        chemicalPixmap.setBlending(Pixmap.Blending.None);

        chemicalTexture = new Texture(chemicalPixmap);

        chemicalSolution.setUpdateChemicalCallback((x, y, c) -> {
            chemicalPixmap.drawPixel(x, y, c.getRGBA8888());
        });

        batch = new SpriteBatch();
        shader = new ShaderProgram(
                Gdx.files.internal("shaders/chemical/vertex.glsl"),
                Gdx.files.internal("shaders/chemical/fragment.glsl"));
        if (!shader.isCompiled()) {
            throw new RuntimeException("Shader compilation failed: " + shader.getLog());
        }
    }

    public void render(float delta) {
        ChemicalSolution chemicalSolution = environment.getChemicalSolution();

        if (chemicalSolution == null || chemicalTexture == null)
            return;

        chemicalTexture.draw(chemicalPixmap, 0, 0);

        batch.enableBlending();
        batch.setProjectionMatrix(camera.combined);
        if (!DebugMode.isModeOrHigher(DebugMode.INTERACTION_INFO)) {
            shader.bind();
            shader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shader.setUniformf("u_blurAmount", 1f);
            batch.setShader(shader);
        } else {
            batch.setShader(null);
        }

        batch.begin();
        float x = -chemicalSolution.getFieldWidth() / 2;
        float y = -chemicalSolution.getFieldHeight() / 2;
        batch.setColor(1, 1, 1, 0.5f);
        batch.draw(chemicalTexture, x, y,
                chemicalSolution.getFieldWidth(), chemicalSolution.getFieldWidth());
        batch.end();
    }

    @Override
    public void dispose() {
        chemicalSolution.setUpdateChemicalCallback(null);
        chemicalTexture.dispose();
        batch.dispose();
        chemicalPixmap.dispose();
        shader.dispose();
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
