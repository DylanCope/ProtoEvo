package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.biology.Cell;
import com.protoevo.core.Particle;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.Settings;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.env.JointsManager;
import com.protoevo.env.Rock;
import com.protoevo.input.ParticleTracker;
import com.protoevo.ui.InputManager;
import com.protoevo.utils.DebugMode;

import static com.protoevo.utils.Utils.lerp;

public class EnvironmentRenderer implements Renderer {

    private final Box2DDebugRenderer debugRenderer;
    private final SpriteBatch batch;
    private final Texture circleTexture;
    private final Sprite jointSprite;
    private final ShapeRenderer shapeRenderer;

    private final Environment environment;
    private final OrthographicCamera camera;
    private final Simulation simulation;
    private final InputManager inputManager;

    public static Sprite loadSprite(String path) {
        Texture texture = new Texture(Gdx.files.internal(path), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);
        return new Sprite(texture);
    }

    public EnvironmentRenderer(OrthographicCamera camera, Simulation simulation, InputManager inputManager) {
        this.camera = camera;
        this.simulation = simulation;
        this.inputManager = inputManager;

        debugRenderer = new Box2DDebugRenderer();
        batch = new SpriteBatch();
        environment = simulation.getEnv();

        FileHandle particleFile = Gdx.files.internal("entity/particle_base_128x128.png");
        circleTexture = new Texture(particleFile, true);
        circleTexture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);

        jointSprite = loadSprite("entity/binding_base_128x128.png");

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
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

        jointSprite.draw(batch);
    }

    public void renderChemicalField() {
        ChemicalSolution chemicalSolution = environment.getChemicalSolution();
        if (chemicalSolution == null) {
            return;
        }
        Texture chemicalTexture = chemicalSolution.getChemicalTexture();
        float x = -chemicalSolution.getFieldWidth() / 2;
        float y = -chemicalSolution.getFieldHeight() / 2;
        batch.draw(chemicalTexture, x, y,
                chemicalSolution.getFieldWidth(), chemicalSolution.getFieldWidth());
    }

    public void render(float delta) {

        ScreenUtils.clear(0, 0.1f, 0.2f, 1);

        // Render Particles
        batch.enableBlending();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        if (Settings.enableChemicalField) {
            renderChemicalField();
        }

        environment.getJointsManager().getParticleBindings().forEach(this::renderJoinedParticles);
        environment.getParticles().forEach(this::drawParticle);
        batch.end();

        // Render rocks
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Rock rock : environment.getRocks()) {
            shapeRenderer.setColor(rock.getColor());
            Vector2[] ps = rock.getPoints();
            shapeRenderer.triangle(ps[0].x, ps[0].y, ps[1].x, ps[1].y, ps[2].x, ps[2].y);
        }
        shapeRenderer.end();

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

        batch.setColor(p.getColor());
        batch.draw(circleTexture, x, y, r, r);
    }

    public void dispose() {
        batch.dispose();
        circleTexture.dispose();
        shapeRenderer.dispose();
    }
}
