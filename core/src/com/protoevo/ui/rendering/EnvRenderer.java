package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.protoevo.biology.Cell;
import com.protoevo.core.Particle;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.env.InteractionsManager;
import com.protoevo.env.JointsManager;
import com.protoevo.env.Rock;
import com.protoevo.input.ParticleTracker;
import com.protoevo.ui.InputManager;
import com.protoevo.utils.DebugMode;

import static com.protoevo.utils.Utils.lerp;

public class EnvRenderer {

    private final Box2DDebugRenderer debugRenderer;
    private final SpriteBatch worldBatch;
    private final SpriteBatch particlesBatch;
    private final Texture circleTexture;
    private final Sprite jointSprite;
    private final ShapeRenderer shapeRenderer;
    private final ShaderProgram vignetteShader;

    private final Environment environment;
    private final OrthographicCamera camera;
    private final Simulation simulation;
    private final InputManager inputManager;

    public static Sprite loadSprite(String path) {
        Texture texture = new Texture(Gdx.files.internal(path), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);
        return new Sprite(texture);
    }

    public EnvRenderer(OrthographicCamera camera, Simulation simulation, InputManager inputManager) {
        this.camera = camera;
        this.simulation = simulation;
        this.inputManager = inputManager;

        debugRenderer = new Box2DDebugRenderer();
        worldBatch = new SpriteBatch();
        particlesBatch = new SpriteBatch();
        environment = simulation.getEnv();

        FileHandle particleFile = Gdx.files.internal("entity/particle_base_128x128.png");
        circleTexture = new Texture(particleFile, true);
        circleTexture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);

        jointSprite = loadSprite("entity/binding_base_128x128.png");

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);

        vignetteShader = new ShaderProgram(
                Gdx.files.internal("shaders/vignette.vsh"),
                Gdx.files.internal("shaders/vignette.fsh"));

        if (!vignetteShader.isCompiled())
            throw new RuntimeException("Shader compilation failed: " + vignetteShader.getLog());
    }

    public void renderJoinedParticles(JointsManager.JoinedParticles joinedParticles) {
        Particle p1 = joinedParticles.particleA;
        Particle p2 = joinedParticles.particleB;

        if (circleNotVisible(p1.getPos(), p1.getRadius())
                && circleNotVisible(p2.getPos(), p2.getRadius())) {
            return;
        }

        Vector2 particle1Position = p1.getBody().getPosition();
        Vector2 particle2Position = p2.getBody().getPosition();
        float r = 1.5f * Math.min(p1.getRadius(), p2.getRadius());
        float d = particle1Position.dst(particle2Position) + r / 2;

        jointSprite.setPosition(particle1Position.x - r / 2, particle1Position.y - r / 2);
        jointSprite.setSize(r, d);
        jointSprite.setOrigin(r / 2, r / 2);

        float angle = 90 + particle1Position.sub(particle2Position).angleDeg();
        jointSprite.setRotation(angle);
        jointSprite.setColor(lerp(p1.getColor(), p2.getColor(), .5f));

        jointSprite.draw(particlesBatch);
    }

    public void renderWorld() {

        camera.update();
        if (inputManager.getParticleTracker().isTracking())
            camera.position.set(inputManager.getParticleTracker().getTrackedParticlePosition());

        // Render Particles
        particlesBatch.enableBlending();
        particlesBatch.setProjectionMatrix(camera.combined);

        particlesBatch.begin();
        environment.getJointsManager().getParticleBindings().forEach(this::renderJoinedParticles);
        environment.getParticles().forEach(this::drawParticle);
        particlesBatch.end();

        // Render rocks
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Rock rock : environment.getRocks()) {
            shapeRenderer.setColor(rock.getColor());
            Vector2[] ps = rock.getPoints();
            shapeRenderer.triangle(ps[0].x, ps[0].y, ps[1].x, ps[1].y, ps[2].x, ps[2].y);
        }
        shapeRenderer.end();
    }

    public void render(float delta) {

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        FrameBuffer fbo = new FrameBuffer(
                Pixmap.Format.RGBA8888,
                (int) camera.viewportWidth,
                (int) camera.viewportHeight,
                false);
        fbo.begin();
        renderWorld();
        fbo.end();

        Sprite worldSprite = new Sprite(fbo.getColorBufferTexture());
        worldSprite.flip(false, true);

        ShockWave shockWave = ShockWave.getInstance();
        if (shockWave.isEnabled()) {
            ShaderProgram shaderProgram = shockWave.getShaderProgram();
            ShaderProgram.pedantic = false;
            shaderProgram.bind();
            Vector2 shockWavePos = shockWave.getShockWavePos();
            Vector3 viewSpacePos = camera.project(new Vector3(shockWavePos.x, shockWavePos.y, 0));
            viewSpacePos.x = viewSpacePos.x / camera.viewportWidth;
            viewSpacePos.y = viewSpacePos.y / camera.viewportHeight;
            shaderProgram.setUniformf("cameraZoom", camera.zoom);
            shaderProgram.setUniformf("resolution", new Vector2(camera.viewportWidth, camera.viewportHeight));
            shaderProgram.setUniformf("time", shockWave.getTime());
            shaderProgram.setUniformf("center", new Vector2(viewSpacePos.x, viewSpacePos.y));
            worldBatch.setShader(shaderProgram);
        }

        worldBatch.begin();
        worldBatch.draw(worldSprite, 0, 0, camera.viewportWidth, camera.viewportHeight);
        worldBatch.end();
        if (worldBatch.getShader() != null)
            worldBatch.setShader(null);

//        ShaderProgram.pedantic = false;
//        vignetteShader.bind();
//        vignetteShader.setUniformMatrix("u_projTrans", camera.combined);
//        vignetteShader.setUniformf("u_resolution", camera.viewportWidth, camera.viewportHeight);
//        vignetteShader.setUniformi("u_tracking", inputManager.getParticleTracker().isTracking() ? 1 : 0);
//
//        Sprite worldSprite = new Sprite(fbo.getColorBufferTexture());
//        worldSprite.flip(false, true);
//        worldBatch.setShader(vignetteShader);
//
//        worldBatch.begin();
//        worldBatch.draw(worldSprite, 0, 0, camera.viewportWidth, camera.viewportHeight);
//        worldBatch.end();

        fbo.dispose();

        if (DebugMode.isDebugModePhysicsDebug()) {
            renderPhysicsDebug();
        }
    }

    public void renderPhysicsDebug() {
        debugRenderer.render(simulation.getEnv().getWorld(), camera.combined);
        ParticleTracker particleTracker = inputManager.getParticleTracker();
        if (particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            shapeRenderer.begin();
            shapeRenderer.setColor(0, 1, 0, 1);
            InteractionsManager interactionsManager = simulation.getEnv().getForceManager();
            float maxDistance = particle.getInteractionRange();
            shapeRenderer.box(
                    particle.getPos().x - maxDistance,
                    particle.getPos().y - maxDistance, 0,
                    2 * maxDistance, 2*maxDistance, 0);

            for (Object other : particle.getContactObjects()) {
                if (other instanceof Particle) {
                    Vector2 otherPos = ((Particle) other).getPos();
                    float otherR = ((Particle) other).getRadius();
                    shapeRenderer.setColor(1, 0, 0, 1);
                    shapeRenderer.circle(otherPos.x, otherPos.y, otherR);
                }
            }

            if (particle instanceof Cell) {
                Cell cell = (Cell) particle;
                shapeRenderer.setColor(0, 0, 1, 1);
                for (Cell other : cell.getAttachedCells()) {
                    Vector2 otherPos = other.getPos();
                    float otherR = other.getRadius();
                    shapeRenderer.setColor(Color.ORANGE);
                    shapeRenderer.circle(otherPos.x, otherPos.y, otherR);
                }
            }

            shapeRenderer.end();
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
