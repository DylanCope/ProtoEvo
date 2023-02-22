package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.core.Particle;
import com.protoevo.core.Simulation;
import com.protoevo.core.SpatialHash;
import com.protoevo.env.*;
import com.protoevo.input.ParticleTracker;
import com.protoevo.ui.SimulationInputManager;
import com.protoevo.ui.shaders.ShaderLayers;
import com.protoevo.utils.DebugMode;
import com.protoevo.utils.Utils;

import java.util.Collection;
import java.util.HashMap;

import static com.protoevo.utils.Utils.lerp;

public class EnvironmentRenderer implements Renderer {

    private final Box2DDebugRenderer debugRenderer;
    private final SpriteBatch batch;
    private final Texture particleTexture;
    private final HashMap<Protozoan, ProtozoaRenderer> protozoaRenderers = new HashMap<>();
    private final Sprite jointSprite;
    private final ShapeRenderer shapeRenderer;
    private final Environment environment;
    private final OrthographicCamera camera;
    private final Simulation simulation;
    private final SimulationInputManager inputManager;
    private final Renderer chemicalsRenderer;
    private final Vector2 tmpVec = new Vector2();
    private final Color tmpColor = new Color();

    public static Sprite loadSprite(String path) {
        Texture texture = new Texture(Gdx.files.internal(path), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);
        return new Sprite(texture);
    }

    public EnvironmentRenderer(OrthographicCamera camera, Simulation simulation, SimulationInputManager inputManager) {
        this.camera = camera;
        this.simulation = simulation;
        this.inputManager = inputManager;
        environment = simulation.getEnv();

        debugRenderer = new Box2DDebugRenderer();
        batch = new SpriteBatch();
        particleTexture = CellTexture.getTexture();

        jointSprite = loadSprite("cell/binding_base_128x128.png");

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);

        if (environment.getChemicalSolution() != null)
            chemicalsRenderer = new ShaderLayers(
                    new ChemicalsRenderer(camera, environment)
//                    new BlurLayer(camera)
            );
        else
            chemicalsRenderer = null;
    }

    public void renderJoinedParticles(JointsManager.Joining joining) {
        Particle p1 = joining.particleA;
        Particle p2 = joining.particleB;

        if (circleNotVisible(p1.getPos(), p1.getRadius())
                && circleNotVisible(p2.getPos(), p2.getRadius())) {
            return;
        }

        Vector2 particle1Position = p1.getPos();
        Vector2 particle2Position = p2.getPos();
        float scale = 1.5f;
        float r = scale * Math.min(p1.getRadius(), p2.getRadius());
        float d = particle1Position.dst(particle2Position) + r / 2;

        jointSprite.setPosition(particle1Position.x - r / 2, particle1Position.y - r / 2);
        jointSprite.setSize(r, d);
        jointSprite.setOrigin(r / 2, r / 2);

        float angle = 90 + tmpVec.set(particle1Position).sub(particle2Position).angleDeg();
        jointSprite.setRotation(angle);

        tmpColor.set(p1.getColor()).add(p2.getColor()).mul(.5f);
        jointSprite.setColor(tmpColor);

        jointSprite.draw(batch);
    }

    public void render(float delta) {
        ScreenUtils.clear(0, 0.1f, 0.2f, .95f);

        synchronized (environment) {
            if (chemicalsRenderer != null)
                chemicalsRenderer.render(delta);

            batch.enableBlending();
            batch.setProjectionMatrix(camera.combined);
            // Render Particles
            batch.begin();
            if (camera.zoom < 3)
                environment.getJointsManager().getParticleBindings()
                        .forEach(this::renderJoinedParticles);
            environment.getParticles().stream()
//                    .parallel()
                    .filter(p -> !circleNotVisible(p.getPos(), p.getRadius()))
                    .iterator()
                    .forEachRemaining(p -> drawParticle(delta, p));
            batch.end();

            protozoaRenderers.entrySet()
                    .removeIf(entry -> entry.getValue().isStale());

            // Render rocks
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (Rock rock : environment.getRocks()) {
                shapeRenderer.setColor(rock.getColor());
                Vector2[] ps = rock.getPoints();
                shapeRenderer.triangle(ps[0].x, ps[0].y, ps[1].x, ps[1].y, ps[2].x, ps[2].y);
            }
            shapeRenderer.end();

            if (DebugMode.isInteractionInfo())
                renderInteractionDebug();
            if (DebugMode.isDebugModePhysicsDebug())
                renderPhysicsDebug();
        }
    }

    public void renderPhysicsDebug() {
        debugRenderer.render(simulation.getEnv().getWorld(), camera.combined);
        shapeRenderer.begin();
        shapeRenderer.setColor(Color.GOLD);
        shapeRenderer.set(ShapeRenderer.ShapeType.Line);

        SpatialHash<Cell> spatialHash = simulation.getEnv().getSpatialHash(Protozoan.class);
        float size = spatialHash.getCellSize();
        for (int i = 0; i < spatialHash.getResolution(); i++) {
            float x = spatialHash.getOriginX() + i * size;
            for (int j = 0; j < spatialHash.getResolution(); j++) {
                float y = spatialHash.getOriginY() + j * size;
                if (boxNotVisible(x, y, size, size))
                    continue;
                shapeRenderer.box(x, y, 0, size, size, 0);
            }
        }
        shapeRenderer.end();
    }

    public void renderInteractionDebug() {
        shapeRenderer.begin();
        ParticleTracker particleTracker = inputManager.getParticleTracker();

        if (particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            shapeRenderer.setColor(0, 1, 0, 1);

            float maxDistance = particle.getInteractionRange();
            shapeRenderer.box(
                    particle.getPos().x - maxDistance,
                    particle.getPos().y - maxDistance, 0,
                    2*maxDistance, 2*maxDistance, 0);

            for (CollisionHandler.FixtureCollision contact : particle.getContacts()) {
                Object other = particle.getOther(contact);
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
                if (cell instanceof Protozoan && protozoaRenderers.containsKey((Protozoan) cell)) {
                    Protozoan protozoan = (Protozoan) cell;
                    ProtozoaRenderer protozoaRenderer = protozoaRenderers.get(protozoan);
                    protozoaRenderer.renderDebug(shapeRenderer);
                }
            }
        }
        shapeRenderer.end();
    }

    private final Vector3 tmpWorldCoordinate = new Vector3();

    public float worldDistanceToScreenDistance(float worldDistance) {
        Vector3 result = camera.project(tmpWorldCoordinate.set(worldDistance, 0, 0));
        return result.len();
    }

    public boolean circleNotVisible(Vector2 pos, float r) {
        return !camera.frustum.boundsInFrustum(pos.x, pos.y, 0, r, r, 0);
    }

    public boolean boxNotVisible(float x, float y, float w, float h) {
        return !camera.frustum.boundsInFrustum(x, y, 0, w, h, 0);
    }

    public void drawParticle(float delta, Particle p) {
        if (p instanceof Protozoan) {
            ProtozoaRenderer protozoanRenderer = protozoaRenderers
                    .computeIfAbsent((Protozoan) p, ProtozoaRenderer::new);
            protozoanRenderer.render(delta, camera, batch);

            Protozoan protozoan = (Protozoan) p;
            Collection<Cell> engulfedCells = protozoan.getEngulfedCells();
            if (engulfedCells != null) {
                for (Cell cell : protozoan.getEngulfedCells()) {
                    float x = cell.getPos().x - cell.getRadius();
                    float y = cell.getPos().y - cell.getRadius();
                    float r = cell.getRadius() * 2;

                    float d2 = tmpVec.set(p.getPos()).dst2(cell.getPos());
                    float dInside = p.getRadius() - cell.getRadius();
                    float alpha = Utils.linearRemap(
                            d2, dInside * dInside, dInside * dInside * 0.75f * 0.75f,
                            1.0f, 0.05f);
                    batch.setColor(cell.getColor().r, cell.getColor().g, cell.getColor().b, alpha);
                    batch.draw(particleTexture, x, y, r, r);
                }
            }
        }
        else {
            float x = p.getPos().x - p.getRadius();
            float y = p.getPos().y - p.getRadius();
            float r = p.getRadius() * 2;
            Color c = p.getColor();
            batch.setColor(c.r, c.g, c.b, 1.0f);
            batch.draw(particleTexture, x, y, r, r);
        }
    }

    public void dispose() {
        batch.dispose();
        particleTexture.dispose();
        shapeRenderer.dispose();
    }
}
