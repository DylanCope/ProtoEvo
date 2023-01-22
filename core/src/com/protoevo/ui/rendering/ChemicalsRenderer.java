package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;

public class ChemicalsRenderer {
    private final Environment environment;
    private final SpriteBatch chemicalBatch;
    private final ShaderProgram chemicalShader;
    private final Texture chemicalTexture;

    public ChemicalsRenderer(Environment environment) {
        this.environment = environment;

        ChemicalSolution chemicalSolution = environment.getChemicalSolution();
        chemicalTexture = new Texture(chemicalSolution.getChemicalPixmap());

        chemicalBatch = new SpriteBatch();
        chemicalShader = new ShaderProgram(
                Gdx.files.internal("shaders/chemical/vertex.glsl"),
                Gdx.files.internal("shaders/chemical/fragment.glsl"));
        if (!chemicalShader.isCompiled()) {
            throw new RuntimeException("Shader compilation failed: " + chemicalShader.getLog());
        }

    }

    public void render(OrthographicCamera camera) {
        ChemicalSolution chemicalSolution = environment.getChemicalSolution();

        if (chemicalSolution == null || chemicalTexture == null)
            return;

        Pixmap chemicalPixmap = chemicalSolution.getChemicalPixmap();
        chemicalTexture.draw(chemicalPixmap, 0, 0);

        chemicalBatch.enableBlending();
        chemicalBatch.setProjectionMatrix(camera.combined);

        chemicalShader.bind();
        chemicalShader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        chemicalBatch.setShader(chemicalShader);

        chemicalBatch.begin();
        float x = -chemicalSolution.getFieldWidth() / 2;
        float y = -chemicalSolution.getFieldHeight() / 2;
        chemicalBatch.draw(chemicalTexture, x, y,
                chemicalSolution.getFieldWidth(), chemicalSolution.getFieldWidth());
        chemicalBatch.end();
    }
}
