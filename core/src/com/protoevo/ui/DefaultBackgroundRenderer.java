package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.env.Environment;
import com.protoevo.settings.EnvironmentSettings;
import com.protoevo.ui.rendering.EnvironmentRenderer;
import com.protoevo.utils.ImageUtils;

public class DefaultBackgroundRenderer {

    private static DefaultBackgroundRenderer instance;

    public static DefaultBackgroundRenderer getInstance() {
        if (instance == null) {
            instance = new DefaultBackgroundRenderer();
        }
        return instance;
    }

    private final OrthographicCamera camera;
    private final Texture texture;
    private final float r;
    private final SpriteBatch batch;
    private final ShaderProgram shader;

    public DefaultBackgroundRenderer() {
        float graphicsHeight = Gdx.graphics.getHeight();
        float graphicsWidth = Gdx.graphics.getWidth();
        EnvironmentSettings settings = DefaultBackgroundGenerator.createBgEnvSettings();
        r = settings.world.radius.get();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, r, r * graphicsHeight / graphicsWidth);
        camera.position.set(0, 0, 0);
        camera.zoom = 1f;

        batch = new SpriteBatch();
        shader = new ShaderProgram(
                Gdx.files.internal("shaders/pause/vertex.glsl"),
                Gdx.files.internal("shaders/pause/fragment.glsl"));
        if (!shader.isCompiled()) {
            throw new RuntimeException("Shader compilation failed: " + shader.getLog());
        }

        texture = ImageUtils.getTexture("bg.png");
    }

    public void render(Float delta) {
        ScreenUtils.clear(EnvironmentRenderer.backgroundColor);
        drawBlurredBackground();
    }

    public void drawBlurredBackground() {
        shader.bind();
        shader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shader.setUniformf("u_blurAmount", 12f);
        shader.setUniformf("u_darkenAmount", 0.75f);
        batch.setShader(shader);

        batch.enableBlending();

        batch.begin();
        batch.setColor(1f, 1f, 1f, 0.7f);
        float scale = 1.25f;
        float l = scale * Math.max(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float x = Gdx.graphics.getWidth() - l;
        float y = Gdx.graphics.getHeight() - l;
        batch.draw(texture, x, y, l, l);
        batch.end();
    }

    public void dispose() {
        batch.dispose();
        shader.dispose();
    }


    public static void disposeInstance() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }
}
