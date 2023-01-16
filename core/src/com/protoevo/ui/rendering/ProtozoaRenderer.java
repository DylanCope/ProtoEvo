package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.nodes.FlagellumAttachment;
import com.protoevo.biology.nodes.LightSensitiveAttachment;
import com.protoevo.biology.nodes.NodeAttachment;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.core.Collidable;
import com.protoevo.core.Particle;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.RenderSettings;
import com.protoevo.env.Rock;
import com.protoevo.utils.ImageUtils;
import com.protoevo.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Function;

public class ProtozoaRenderer {

    enum InteriorTexture {
        GOLGI(0, 2, "golgi", "golgi_2", "golgi_3"),
        NUCLEUS(1, 1, "nucleus_1", "nucleus_2"),
        RIBOSOMES(0, 3, "ribosomes_1", "ribosomes_2", "ribosomes_3"),
        MITOCHONDRIA(0, 2, "mitochondria_1"),
        MICROFILAMENT(0, 3, "microfilament_1", "microfilament_2", "microfilament_3");

        private final BufferedImage[] textures;
        private final int minCount, maxCount;
        InteriorTexture(int minCount, int maxCount, String...textureNames) {
            this.minCount = minCount;
            this.maxCount = maxCount;
            textures = new BufferedImage[textureNames.length];
            for (int i = 0; i < textureNames.length; i++) {
                textures[i] = ImageUtils.loadImage("cell/cell_interior/" + textureNames[i] + ".png");
            }
        }

        public int getMaxCount() {
            return maxCount;
        }

        public int getMinCount() {
            return minCount;
        }

        public BufferedImage randomTexture() {
            return textures[Simulation.RANDOM.nextInt(0, textures.length)];
        }
    }

    private static final Map<Class<? extends NodeAttachment>, Function<SurfaceNode, NodeRenderer>> nodeRendererMap =
        new HashMap<Class<? extends NodeAttachment>, Function<SurfaceNode, NodeRenderer>>(){
            {
                put(FlagellumAttachment.class, FlagellumRenderer::new);
                put(LightSensitiveAttachment.class, LightSensitiveNodeRenderer::new);
            }
        };

    private final Protozoan protozoan;
    private final Sprite cellSprite;
    private final Map<SurfaceNode, NodeRenderer> nodeRenderers;

    public ProtozoaRenderer(Protozoan protozoan) {
        this.protozoan = protozoan;
        cellSprite = ImageUtils.convertToSprite(generateCellImage());
        nodeRenderers = new HashMap<>(protozoan.getSurfaceNodes().size());
    }

    public NodeRenderer createNodeRenderer(SurfaceNode node) {
        Optional<? extends NodeAttachment> maybeAttachment = node.getAttachment();
        return maybeAttachment
                .map(NodeAttachment::getClass)
                .map(nodeRendererMap::get)
                .map(f -> f.apply(node))
                .orElse(new NodeRenderer(node));
    }

    public void render(float delta, OrthographicCamera camera, SpriteBatch batch) {
        Vector2 pos = protozoan.getPos();
        float x = pos.x - protozoan.getRadius();
        float y = pos.y - protozoan.getRadius();
        float cellAngle = (float) Math.toDegrees(protozoan.getBody().getAngle());
        float size = protozoan.getRadius() * 2;

        if (RenderSettings.cameraZoomForCellDetails > camera.zoom) {
            nodeRenderers.entrySet().removeIf(e -> e.getValue().isStale());
            for (SurfaceNode node : protozoan.getSurfaceNodes()) {
                nodeRenderers.computeIfAbsent(node, this::createNodeRenderer)
                        .render(delta, batch);
            }
        }

        cellSprite.setColor(protozoan.getColor());
        cellSprite.setOriginCenter();
        cellSprite.setRotation(cellAngle);
        cellSprite.setPosition(x, y);
        cellSprite.setSize(size, size);
        cellSprite.draw(batch);
    }

    public void renderDebug(ShapeRenderer sr) {
        int i = 0;

        sr.setColor(1, 0, 1, 1);
        for (Object obj : protozoan.getInteractionQueue()) {
            if (obj instanceof Particle) {
                Particle particle = (Particle) obj;
                sr.circle(particle.getPos().x, particle.getPos().y, particle.getRadius() * 1.1f);
            } else if (obj instanceof Rock) {
                Rock rock = (Rock) obj;
                for (Vector2[] edge : rock.getEdges())
                    sr.line(edge[0], edge[1]);
            }
        }

        for (SurfaceNode surfaceNode : protozoan.getSurfaceNodes()) {
            if (i == 0)
                sr.setColor(0, 1, 0, 1);
            else if (i == 1)
                sr.setColor(0, 0, 1, 1);
            else if (i == protozoan.getSurfaceNodes().size() - 1)
                sr.setColor(1, 0, 0, 1);
            else
                sr.setColor(1, 1, 1, 1);
            i++;
            Vector2 pos = protozoan.getPos();
            Vector2 dv = surfaceNode.getRelativePos();
            sr.circle(pos.x + dv.x, pos.y + dv.y, protozoan.getRadius() / 10f, 15);

            Optional<NodeAttachment> maybeAttachment = surfaceNode.getAttachment();
            if (maybeAttachment.isPresent() && maybeAttachment.get() instanceof FlagellumAttachment) {
                FlagellumAttachment attachment = (FlagellumAttachment) maybeAttachment.get();
                float maxThrust = ProtozoaSettings.maxProtozoaThrust;
                Vector2 thrust = attachment.getThrustVector().cpy().setLength(protozoan.getRadius()*1.5f);
                float mag = Utils.linearRemap(thrust.len(), 0, maxThrust, 0, 1.5f);
                sr.setColor(0, 1, 0, 1);
                Vector2 v = thrust.cpy().setLength(mag * protozoan.getRadius());
                sr.line(pos.x, pos.y, pos.x + v.x, pos.y + v.y);
            }
            if (maybeAttachment.isPresent() && maybeAttachment.get() instanceof LightSensitiveAttachment) {
                LightSensitiveAttachment attachment = (LightSensitiveAttachment) maybeAttachment.get();
                sr.setColor(1, 0, 0, 1);

                SurfaceNode node = attachment.getNode();
                Cell cell = node.getCell();
                for (int rayIdx = 0; rayIdx < LightSensitiveAttachment.nRays; rayIdx++) {
                    Vector2[] ray = attachment.nextRay();
                    sr.line(ray[0], ray[1]);
                    for (Object o : cell.getInteractionQueue()) {
                        if (o instanceof Collidable) {
                            Vector2[] collisions = attachment.handleCollidable((Collidable) o);
                            if (collisions == null)
                                continue;
                            sr.setColor(1, 0, 0, 1);
                            for (Vector2 collision : collisions)
                                sr.circle(collision.x, collision.y,
                                        cell.getRadius() / 15f, 15);
                        }
                    }
                }
            }
        }
    }

    public boolean isStale() {
        return protozoan.isDead();
    }

    public static BufferedImage generateCellImage() {
        BufferedImage base = ParticleTexture.getBufferedImage();

        ArrayList<InteriorTexture> types = new ArrayList<>();
        Random random = new Random();
        for (InteriorTexture type : InteriorTexture.values()) {
            int count = random.nextInt(type.getMinCount(),  type.getMaxCount() + 1);
            for (int i = 0; i < count; i++) {
                types.add(type);
            }
        }
        Collections.shuffle(types);

        BufferedImage newImage = new BufferedImage(
                base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = newImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(base, null, 0, 0);
        double dt = 2 * Math.PI / types.size();
        for (int i = 0; i < types.size(); i++) {
            double t = i * dt + random.nextDouble(-dt / 3, dt / 3);
            BufferedImage texture = types.get(i).randomTexture();
            texture = ImageUtils.scaleImage(texture, random.nextDouble(0.6, 1));
//            if (random.nextBoolean())
//                texture = ImageUtils.flipImageHorizontally(texture);
            texture = ImageUtils.rotateImageByRadians(texture, t);
//            if (random.nextBoolean())
//                texture = ImageUtils.flipImageVertically(texture);

            g2d.drawImage(texture, null,
                    (newImage.getWidth() - texture.getWidth()) / 2,
                    (newImage.getHeight() - texture.getHeight()) / 2);
        }
        return newImage;
    }

}