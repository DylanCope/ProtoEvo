package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.env.Environment;
import com.protoevo.physics.*;
import com.protoevo.env.Rock;
import com.protoevo.input.ParticleTracker;
import com.protoevo.physics.box2d.Box2DParticle;
import com.protoevo.physics.Collision;
import com.protoevo.physics.box2d.Box2DPhysics;
import com.protoevo.ui.SimulationInputManager;
import com.protoevo.ui.UIStyle;
import com.protoevo.utils.DebugMode;
import com.protoevo.utils.Utils;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

public class EnvironmentRenderer implements Renderer {
    public static Color backgroundColor = new Color(0, 0.1f, 0.2f, 1);
    private final Box2DDebugRenderer physicsDebugRenderer;
    private final SpriteBatch batch;
    private final Texture particleTexture;
    private final HashMap<Protozoan, ProtozoaRenderer> protozoaRenderers = new HashMap<>();
    private final Sprite jointSprite;
    private final ShapeRenderer debugRenderer;
    private final ShapeDrawer shapeDrawer;
    private final Environment environment;
    private final OrthographicCamera camera;
    private final SimulationInputManager inputManager;
    private final ChemicalsRenderer chemicalsRenderer;
    private final LightRenderer lightRenderer;
    private final Vector2 tmpVec = new Vector2();
    private final Color tmpColor = new Color();

    public static Sprite loadSprite(String path) {
        Texture texture = new Texture(Gdx.files.internal(path), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);
        return new Sprite(texture);
    }

    public EnvironmentRenderer(OrthographicCamera camera, Environment environment, SimulationInputManager inputManager) {
        this.camera = camera;
        this.inputManager = inputManager;
        this.environment = environment;

        physicsDebugRenderer = new Box2DDebugRenderer();
        batch = new SpriteBatch();
        particleTexture = CellTexture.getTexture();

        shapeDrawer = new ShapeDrawer(batch, new TextureRegion(UIStyle.getWhite1x1()));

        jointSprite = loadSprite("cell/binding_base_128x128.png");

        debugRenderer = new ShapeRenderer();
        debugRenderer.setAutoShapeType(true);

        if (environment.getChemicalSolution() != null)
            chemicalsRenderer = new ChemicalsRenderer(camera, environment);
        else
            chemicalsRenderer = null;

        lightRenderer = new LightRenderer(camera, environment);
    }

    public void renderJoinedParticles(Joining joining) {
        Optional<Particle> maybeP1 = joining.getParticleA();
        Optional<Particle> maybeP2 = joining.getParticleB();
        if (!maybeP1.isPresent() || !maybeP2.isPresent())
            return;

        Particle p1 = maybeP1.get();
        Particle p2 = maybeP2.get();

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

        tmpColor.set(p1.getUserData(Cell.class).getColor()).lerp(p2.getUserData(Cell.class).getColor(), .5f);
        tmpColor.a = 1f;
        jointSprite.setColor(tmpColor);

        jointSprite.draw(batch);
    }

    public void render(float delta) {
        ScreenUtils.clear(backgroundColor);
        if (chemicalsRenderer != null)
            chemicalsRenderer.render(delta);

        batch.enableBlending();
        batch.setProjectionMatrix(camera.combined);
        // Render Particles
        batch.begin();
        if (camera.zoom < 3)
            environment.getJointsManager().getJoinings()
                    .forEach(this::renderJoinedParticles);
        environment.getParticles()
                .filter(p -> !circleNotVisible(p.getPos(), p.getRadius()))
                .iterator()
                .forEachRemaining(p -> drawParticle(delta, p));
        batch.end();

        lightRenderer.render(delta);

        batch.begin();
        renderRocks();
        batch.end();

        protozoaRenderers.entrySet()
                .removeIf(entry -> entry.getValue().isStale());


        if (DebugMode.isInteractionInfo())
            renderInteractionDebug();
        if (DebugMode.isDebugModePhysicsDebug())
            renderPhysicsDebug();

    }

    public void renderRocks() {
        shapeDrawer.update();
        for (Rock rock : environment.getRocks()) {
            shapeDrawer.setColor(rock.getColor());
            Vector2[] ps = rock.getPoints();
            shapeDrawer.filledTriangle(ps[0].x, ps[0].y, ps[1].x, ps[1].y, ps[2].x, ps[2].y);

            Color rockColor = rock.getColor();
            float k = 0.8f;
            shapeDrawer.setColor(rockColor.r * k, rockColor.g * k, rockColor.b * k, rockColor.a);
            for (int i = 0; i < rock.getEdges().length; i++) {
                if (rock.isEdgeAttached(i))
                    continue;
                Vector2[] edge = rock.getEdge(i);
                float w = 0.05f * Environment.settings.worldgen.maxRockSize.get();
                Vector2 dir = edge[1].cpy().sub(edge[0]).setLength(w / 2f);
                Vector2 start = edge[0].cpy().sub(dir);
                Vector2 end = edge[1].cpy().add(dir);
                shapeDrawer.line(start, end, w);
            }
        }

//        g.setStroke(new BasicStroke(toRenderSpace(0.02f * Settings.maxRockSize)));
////			g.drawPolygon(xPoints, yPoints, screenPoints.length);
//        for (int i = 0; i < rock.getEdges().length; i++) {
//            if (rock.isEdgeAttached(i))
//                continue;
//            Vector2[] edge = rock.getEdge(i);
//            Vector2 start = toRenderSpace(edge[0]);
//            Vector2 end = toRenderSpace(edge[1]);
//            g.drawLine((int) start.getX(), (int) start.getY(),
//                    (int) end.getX(), (int) end.getY());
//        }
    }

    public void renderPhysicsDebug() {
        Physics physics = environment.getPhysics();
        if (physics instanceof Box2DPhysics) {
            physicsDebugRenderer.render(((Box2DPhysics) physics).getWorld(), camera.combined);
        }
        debugRenderer.begin();
        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.setColor(Color.GOLD);
        debugRenderer.set(ShapeRenderer.ShapeType.Line);

        SpatialHash<Cell> spatialHash = environment.getSpatialHash(Protozoan.class);
        float size = spatialHash.getChunkSize();
        for (int i = 0; i < spatialHash.getResolution(); i++) {
            float x = spatialHash.getOriginX() + i * size;
            for (int j = 0; j < spatialHash.getResolution(); j++) {
                float y = spatialHash.getOriginY() + j * size;
                if (boxNotVisible(x, y, size, size))
                    continue;
                debugRenderer.box(x, y, 0, size, size, 0);
            }
        }

        debugRenderer.end();
    }

    public void renderInteractionDebug() {
        if (inputManager == null)
            return;

        debugRenderer.begin();
        debugRenderer.setProjectionMatrix(camera.combined);
        ParticleTracker particleTracker = inputManager.getParticleTracker();

        if (particleTracker.isTracking()) {
            Particle particle = particleTracker.getTrackedParticle();
            debugRenderer.setColor(0, 1, 0, 1);

            float maxDistance = particle.getUserData(Cell.class).getInteractionRange();
            debugRenderer.box(
                    particle.getPos().x - maxDistance,
                    particle.getPos().y - maxDistance, 0,
                    2*maxDistance, 2*maxDistance, 0);

            for (Collision contact : particle.getContacts()) {
                Object other = contact.getOther(particle);
                if (other instanceof Particle) {
                    Particle otherParticle = (Particle) other;
                    Vector2 otherPos = otherParticle.getPos();
                    float otherR = otherParticle.getRadius();
                    debugRenderer.setColor(1, 0, 0, 1);
                    debugRenderer.circle(otherPos.x, otherPos.y, otherR);
                }
            }

            if (particle.getUserData() instanceof Cell) {
                Cell cell = particle.getUserData(Cell.class);
                debugRenderer.setColor(0, 0, 1, 1);
                for (Long otherId : cell.getAttachedCellIDs()) {
                    Optional<Cell> maybeOther = cell.getCell(otherId);
                    if (!maybeOther.isPresent())
                        continue;
                    Cell other = maybeOther.get();
                    Vector2 otherPos = other.getPos();
                    float otherR = other.getRadius();
                    debugRenderer.setColor(Color.ORANGE);
                    debugRenderer.circle(otherPos.x, otherPos.y, otherR);
                }
                if (cell instanceof Protozoan && protozoaRenderers.containsKey((Protozoan) cell)) {
                    Protozoan protozoan = (Protozoan) cell;
                    ProtozoaRenderer protozoaRenderer = protozoaRenderers.get(protozoan);
                    protozoaRenderer.renderDebug(debugRenderer);
                }
            }
        }
        debugRenderer.end();
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
        if (p.getUserData() instanceof Protozoan) {
            Protozoan protozoan = p.getUserData(Protozoan.class);
            ProtozoaRenderer protozoanRenderer = protozoaRenderers
                    .computeIfAbsent(protozoan, ProtozoaRenderer::new);
            protozoanRenderer.render(delta, camera, batch);

            Collection<Cell> engulfedCells = protozoan.getEngulfedCells();
            if (engulfedCells != null) {
                for (Cell cell : protozoan.getEngulfedCells()) {
                    float x = cell.getPos().x - cell.getRadius();
                    float y = cell.getPos().y - cell.getRadius();
                    float r = cell.getRadius() * 2;

                    float d2 = tmpVec.set(p.getPos()).dst2(cell.getPos());
                    float dInside = p.getRadius() - cell.getRadius();
                    float alpha = Utils.clampedLinearRemap(
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
            Color c = p.getUserData(Cell.class).getColor();
            batch.setColor(c.r, c.g, c.b, 1.0f);
            batch.draw(particleTexture, x, y, r, r);
        }
    }

    public void dispose() {
        CellTexture.dispose();
        ProtozoaRenderer.InteriorTexture.dispose();
        NodeRenderer.dispose();
        PhotoreceptorRenderer.disposeStaticSprite();
        SpikeRenderer.disposeStaticSprite();
        FlagellumRenderer.disposeAnimation();
        batch.dispose();
        particleTexture.dispose();
        physicsDebugRenderer.dispose();
        debugRenderer.dispose();
        chemicalsRenderer.dispose();
    }
}
