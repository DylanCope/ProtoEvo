package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.nodes.Flagellum;
import com.protoevo.biology.nodes.NodeAttachment;
import com.protoevo.biology.nodes.Photoreceptor;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.core.Particle;
import com.protoevo.core.Simulation;
import com.protoevo.env.Rock;
import com.protoevo.settings.RenderSettings;
import com.protoevo.utils.Geometry;
import com.protoevo.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

public class ProtozoaRenderer {

    enum InteriorTexture {
        GOLGI(0, 2, 0.1f, 0.3f,
                "golgi_1", "golgi_2", "golgi_3"),
        NUCLEUS(1, 1, 0.5f, 0.7f,
                "nucleus_1", "nucleus_2", "nucleus_3"),
        RIBOSOMES(0, 3, 0.05f, 0.1f,
                "ribosomes_1", "ribosomes_2", "ribosomes_3"),
        MITOCHONDRIA(0, 2, 0.2f, 0.4f,
                "mitochondria_1", "mitochondria_2"),
        MICROFILAMENT(0, 3, 0.1f, 0.2f,
                "microfilament_1", "microfilament_2", "microfilament_3");

        private final Texture[] textures;
        private final int minCount, maxCount;
        private final float minScale, maxScale;

        InteriorTexture(
                int minCount, int maxCount,
                float minScale, float maxScale,
                String...textureNames) {
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.minScale = minScale;
            this.maxScale = maxScale;
            textures = new Texture[textureNames.length];
            for (int i = 0; i < textureNames.length; i++) {
                textures[i] = new Texture(
                        "cell/cell_interior/" + textureNames[i] + ".png");
                textures[i].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                textures[i].setAnisotropicFilter(16);
            }
        }

        public int getMaxCount() {
            return maxCount;
        }

        public int getMinCount() {
            return minCount;
        }

        public Texture randomTexture() {
            return textures[Simulation.RANDOM.nextInt(0, textures.length)];
        }

        public float randomScale() {
            return Simulation.RANDOM.nextFloat(minScale, maxScale);
        }
    }

    private static final Map<Class<? extends NodeAttachment>, Function<SurfaceNode, NodeRenderer>> nodeRendererMap =
        new HashMap<Class<? extends NodeAttachment>, Function<SurfaceNode, NodeRenderer>>(){
            {
                put(Flagellum.class, FlagellumRenderer::new);
                put(Photoreceptor.class, PhotoreceptorRenderer::new);
            }
        };

    private static class InteriorElement {
        private final Sprite elementSprite;
        private float angle;
        private final float scale;
        private Vector2 position;
        private final Protozoan protozoan;

        public InteriorElement(InteriorTexture texture, Protozoan protozoan) {
            this.protozoan = protozoan;
            scale = texture.randomScale();
            this.elementSprite = new Sprite(texture.randomTexture());
            this.angle = MathUtils.random((float) (2 * Math.PI));
            this.position = Geometry.randomPointInCircle(1 - scale);
        }

        public void draw(float delta, OrthographicCamera camera, SpriteBatch batch) {
//            angle += delta * 0.5f;
//            position = Geometry.rotate(position, delta * 0.5f);
            float cellAngle = protozoan.getAngle();

            float criticalZoom = RenderSettings.cameraZoomForCellDetails;
            float a = Utils.linearRemap(
                    camera.zoom, criticalZoom, .5f * criticalZoom, 0, 1f);
            Color c = protozoan.getColor();
            elementSprite.setColor(c.r, c.g, c.b, a);
            elementSprite.setOriginCenter();
            elementSprite.setRotation(angle);
            elementSprite.setPosition(
                    (float) (protozoan.getPos().x + Math.cos(cellAngle + angle) * position.x * protozoan.getRadius() * 0.7f),
                    (float) (protozoan.getPos().y + Math.sin(cellAngle + angle) * position.y * protozoan.getRadius() * 0.7f));
//            elementSprite.setSize(size, size);
            float w = scale * protozoan.getRadius();
            float h = w * elementSprite.getHeight() / elementSprite.getWidth();
            elementSprite.setSize(w, h);
            elementSprite.draw(batch);
        }
    }

    private static final Sprite spikeTexture = new Sprite(new Texture("cell/spike.png"));
    private final Protozoan protozoan;
//    private Sprite detailedSprite = null;
//    private BufferedImage protozoanImage = null;
//    private boolean isGeneratingImage = false;
    private final Map<SurfaceNode, NodeRenderer> nodeRenderers;
    private final ArrayList<InteriorElement> interiorElements = new ArrayList<>();

    public ProtozoaRenderer(Protozoan protozoan) {
        this.protozoan = protozoan;
        nodeRenderers = new HashMap<>(protozoan.getSurfaceNodes().size());

        Random random = new Random();
        for (InteriorTexture type : InteriorTexture.values()) {
            int count = random.nextInt(type.getMinCount(),  type.getMaxCount() + 1);
            for (int i = 0; i < count; i++) {
                interiorElements.add(new InteriorElement(type, protozoan));
            }
        }
    }

    public NodeRenderer createNodeRenderer(SurfaceNode node) {
        NodeAttachment maybeAttachment = node.getAttachment();
        if (maybeAttachment == null || !nodeRendererMap.containsKey(maybeAttachment.getClass())) {
            return new NodeRenderer(node);
        }
        return nodeRendererMap.get(maybeAttachment.getClass()).apply(node);
    }

    public void render(float delta, OrthographicCamera camera, SpriteBatch batch) {
        Vector2 pos = protozoan.getPos();
        float x = pos.x - protozoan.getRadius();
        float y = pos.y - protozoan.getRadius();
        float cellAngle = (float) Math.toDegrees(protozoan.getAngle());
        float size = protozoan.getRadius() * 2;

        Sprite cellSprite = CellTexture.getSprite();

        for (InteriorElement element : interiorElements) {
            element.draw(delta, camera, batch);
        }

        nodeRenderers.entrySet().removeIf(e -> e.getValue().isStale());
        for (SurfaceNode node : protozoan.getSurfaceNodes()) {
            nodeRenderers.computeIfAbsent(node, this::createNodeRenderer)
                    .render(delta, batch);
        }

        cellSprite.setColor(protozoan.getColor());
        cellSprite.setPosition(x, y);
        cellSprite.setSize(size, size);
        cellSprite.draw(batch);

//        if (RenderSettings.cameraZoomForCellDetails > camera.zoom) {
//            for (InteriorElement element : interiorElements) {
//                element.draw(delta, camera, batch);
//            }
//        }
    }

    public void renderDebug(ShapeRenderer sr) {
        int i = 0;

        sr.setColor(1, 0, 1, 1);
        for (Object obj : protozoan.getInteractionQueue()) {
            if (obj instanceof Particle) {
                Particle particle = (Particle) obj;
                sr.circle(particle.getPos().x,
                          particle.getPos().y,
                          particle.getRadius() * 1.1f, 15);
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

            nodeRenderers.computeIfAbsent(surfaceNode, this::createNodeRenderer)
                    .renderDebug(sr);
        }
    }

    public boolean isStale() {
        boolean stale = protozoan.isDead();
        if (stale)
            dispose();
        return stale;
    }

    public void dispose() {
//        if (detailedSprite != null) {
//            detailedSprite.getTexture().dispose();
//            detailedSprite = null;
//        }
        for (NodeRenderer nodeRenderer : nodeRenderers.values()) {
            nodeRenderer.dispose();
        }
        nodeRenderers.clear();
    }

}
