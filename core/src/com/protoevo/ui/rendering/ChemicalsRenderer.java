package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.utils.Colour;
import com.protoevo.utils.DebugMode;

import java.util.stream.IntStream;

public class ChemicalsRenderer implements Renderer {
    private final Environment environment;
    private final ChemicalSolution chemicalSolution;
    private final SpriteBatch chemicalBatch;
    private final ShaderProgram chemicalShader;
    private final Texture chemicalTexture;
    private Pixmap chemicalPixmap;
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

        chemicalBatch = new SpriteBatch();
        chemicalShader = new ShaderProgram(
                Gdx.files.internal("shaders/chemical/vertex.glsl"),
                Gdx.files.internal("shaders/chemical/fragment.glsl"));
        if (!chemicalShader.isCompiled()) {
            throw new RuntimeException("Shader compilation failed: " + chemicalShader.getLog());
        }
    }

    public void render(float delta) {
        ChemicalSolution chemicalSolution = environment.getChemicalSolution();

        if (chemicalSolution == null || chemicalTexture == null)
            return;

        chemicalTexture.draw(chemicalPixmap, 0, 0);

        chemicalBatch.enableBlending();
        chemicalBatch.setProjectionMatrix(camera.combined);
        if (!DebugMode.isModeOrHigher(DebugMode.INTERACTION_INFO)) {
            chemicalShader.bind();
            chemicalShader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            chemicalBatch.setShader(chemicalShader);
        } else {
            chemicalBatch.setShader(null);
        }

        chemicalBatch.begin();
        float x = -chemicalSolution.getFieldWidth() / 2;
        float y = -chemicalSolution.getFieldHeight() / 2;
        chemicalBatch.setColor(1, 1, 1, 0.5f);
        chemicalBatch.draw(chemicalTexture, x, y,
                chemicalSolution.getFieldWidth(), chemicalSolution.getFieldWidth());
        chemicalBatch.end();
    }

    @Override
    public void dispose() {
        chemicalSolution.setUpdateChemicalCallback(null);
        chemicalTexture.dispose();
        chemicalBatch.dispose();
        chemicalPixmap.dispose();
        chemicalShader.dispose();
    }
}
