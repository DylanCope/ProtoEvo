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

import java.nio.ByteBuffer;

public class ChemicalsRenderer implements Renderer {
    private Environment environment;
    private final ChemicalSolution chemicalSolution;
    private final SpriteBatch batch;
    private final ShaderProgram shader;
    private final Texture chemicalTexture;
    private final Pixmap chemicalPixmap;
    private final ByteBuffer pixmapBuffer;
    private final int chemWidth, chemHeight;
    private final OrthographicCamera camera;

    public ChemicalsRenderer(OrthographicCamera camera, Environment environment) {
        this.environment = environment;
        this.camera = camera;

        chemicalSolution = environment.getChemicalSolution();
        chemWidth = chemicalSolution.getNXCells();
        chemHeight = chemicalSolution.getNYCells();

        chemicalPixmap = new Pixmap(chemWidth, chemHeight, Pixmap.Format.RGBA8888);
        chemicalPixmap.setBlending(Pixmap.Blending.None);
        pixmapBuffer = chemicalPixmap.getPixels();

        chemicalTexture = new Texture(chemicalPixmap);

        // No more per-pixel callback. The old design fired a JNI
        // Pixmap.drawPixel call on every chemical-cell set() — at 1024×1024
        // resolution with a diffuse touching every cell, that was ~1M JNI
        // hops per chem update, all on the sim thread but blocking the
        // shared pixmap object the render thread reads. Now sim just flips
        // a dirty bit and render bulk-copies the float array into the
        // pixmap's underlying ByteBuffer in one sweep.

        batch = new SpriteBatch();
        shader = new ShaderProgram(
                Gdx.files.internal("shaders/chemical/vertex.glsl"),
                Gdx.files.internal("shaders/chemical/fragment.glsl"));
        if (!shader.isCompiled()) {
            throw new RuntimeException("Shader compilation failed: " + shader.getLog());
        }
    }

    private void refreshPixmap() {
        Colour[][] colours = chemicalSolution.getImage();
        ByteBuffer buf = pixmapBuffer;
        buf.position(0);
        for (int y = 0; y < chemHeight; y++) {
            for (int x = 0; x < chemWidth; x++) {
                Colour c = colours[x][y];
                int r = clamp255((int) (c.r * 255));
                int g = clamp255((int) (c.g * 255));
                int b = clamp255((int) (c.b * 255));
                int a = clamp255((int) (c.a * 255));
                buf.put((byte) r);
                buf.put((byte) g);
                buf.put((byte) b);
                buf.put((byte) a);
            }
        }
        buf.position(0);
    }

    private static int clamp255(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    public void render(float delta) {
        ChemicalSolution chemicalSolution = environment.getChemicalSolution();

        if (chemicalSolution == null || chemicalTexture == null)
            return;

        // Refresh pixmap from the chemical array only when sim flagged a
        // change. Most frames the chemicals didn't actually update this
        // tick (sim batches them to once per render at most), so we skip
        // the whole bulk-copy and texture upload. clearDirty AFTER read so
        // a concurrent set() during the copy still leaves us dirty for the
        // next frame.
        if (chemicalSolution.isDirty()) {
            refreshPixmap();
            chemicalSolution.clearDirty();
            chemicalTexture.draw(chemicalPixmap, 0, 0);
        }

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
