package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.core.Particle;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.env.Rock;
import com.protoevo.ui.InputManager;
import com.protoevo.utils.DebugMode;

public class Renderer {

    private Box2DDebugRenderer debugRenderer;
    private final SpriteBatch worldBatch;
    private final SpriteBatch particlesBatch;
    private final Texture circleTexture;
    private final ShapeRenderer shapeRenderer;
    private final ShaderProgram vignetteShader;

    private final Environment environment;
    private final OrthographicCamera camera;
    private final Simulation simulation;
    private final InputManager inputManager;

    public Renderer(OrthographicCamera camera, Simulation simulation, InputManager inputManager) {
        this.camera = camera;
        this.simulation = simulation;
        this.inputManager = inputManager;

        debugRenderer = new Box2DDebugRenderer();
        worldBatch = new SpriteBatch();
        particlesBatch = new SpriteBatch();
        environment = simulation.getEnv();

        FileHandle file = Gdx.files.internal("entity/particle_base_128x128.png");
        circleTexture = new Texture(file, true);
        circleTexture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);

        vignetteShader = new ShaderProgram(
                Gdx.files.internal("shaders/vignette.vsh"),
                Gdx.files.internal("shaders/vignette.fsh"));

        if (!vignetteShader.isCompiled())
            throw new RuntimeException("Shader compilation failed: " + vignetteShader.getLog());
    }

    public void render(float delta) {

        FrameBuffer fbo = new FrameBuffer(
                Pixmap.Format.RGBA8888,
                (int) camera.viewportWidth,
                (int) camera.viewportHeight,
                false);
        fbo.begin();

        ScreenUtils.clear(0, 0.1f, 0.2f, 1);

        camera.update();
        if (inputManager.getParticleTracker().isTracking())
            camera.position.set(inputManager.getParticleTracker().getTrackedParticlePosition());

        particlesBatch.enableBlending();
        particlesBatch.setProjectionMatrix(camera.combined);

        particlesBatch.begin();
        environment.getParticles().forEach(this::drawParticle);
        particlesBatch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Rock rock : environment.getRocks()) {
            shapeRenderer.setColor(rock.getColor());
            Vector2[] ps = rock.getPoints();
            shapeRenderer.triangle(ps[0].x, ps[0].y, ps[1].x, ps[1].y, ps[2].x, ps[2].y);
        }
        shapeRenderer.end();

        fbo.end();

        ShaderProgram.pedantic = false;
        vignetteShader.bind();
        vignetteShader.setUniformMatrix("u_projTrans", camera.combined);
        vignetteShader.setUniformf("u_resolution", camera.viewportWidth, camera.viewportHeight);
        vignetteShader.setUniformi("u_tracking", inputManager.getParticleTracker().isTracking() ? 1 : 0);

        Sprite worldSprite = new Sprite(fbo.getColorBufferTexture());
        worldSprite.flip(false, true);
        worldBatch.setShader(vignetteShader);

        worldBatch.begin();
        worldBatch.draw(worldSprite, 0, 0, camera.viewportWidth, camera.viewportHeight);
        worldBatch.end();

        fbo.dispose();

        if (DebugMode.isDebugModePhysicsDebug()) {
            debugRenderer.render(simulation.getEnv().getWorld(), camera.combined);
        }
    }

    public boolean circleNotVisible(Vector2 pos, float r) {
        return !camera.frustum.boundsInFrustum(pos.x, pos.y, 0, r, r, 0);
    }

    public void drawParticle(Particle p) {

        if (circleNotVisible(p.getPos(), p.getRadius())) {
            return;
        }

        float x = p.getPos().x - p.getRadius();
        float y = p.getPos().y - p.getRadius();
        float r = p.getRadius() * 2;

        particlesBatch.setColor(p.getColor());
        particlesBatch.draw(circleTexture, x, y, r, r);
    }

    public void dispose() {
        worldBatch.dispose();
        particlesBatch.dispose();
        circleTexture.dispose();
        shapeRenderer.dispose();
        vignetteShader.dispose();
    }
}
