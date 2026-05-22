package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.nodes.*;
import com.protoevo.utils.ImageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class NodeRenderer {
    private static Sprite nodeEmptySprite = null;

    // Factory map shared by every cell type — picks the right NodeRenderer
    // subclass for the attachment a node is currently expressing. Lives here
    // (rather than ProtozoaRenderer) because plants now grow nodes too.
    private static final Map<Class<? extends NodeAttachment>,
            Function<SurfaceNode, NodeRenderer>> rendererFactory =
        new HashMap<Class<? extends NodeAttachment>,
                Function<SurfaceNode, NodeRenderer>>() {{
            put(Flagellum.class, FlagellumRenderer::new);
            put(Photoreceptor.class, PhotoreceptorRenderer::new);
            put(Spike.class, SpikeRenderer::new);
            put(AdhesionReceptor.class, AdhesionRenderer::new);
        }};

    public static NodeRenderer createFor(SurfaceNode node) {
        NodeAttachment attachment = node.getAttachment();
        if (attachment == null || !rendererFactory.containsKey(attachment.getClass()))
            return new NodeRenderer(node);
        return rendererFactory.get(attachment.getClass()).apply(node);
    }

    protected static Sprite getNodeEmptySprite() {
        if (nodeEmptySprite == null)
            nodeEmptySprite = ImageUtils.loadSprite("cell/nodes/node_empty.png");
        return nodeEmptySprite;
    }

    private static Map<Class<? extends NodeAttachment>, Sprite> attachmentSprites = null;

    public static Map<Class<? extends NodeAttachment>, Sprite> getAttachmentSprites() {
        if (attachmentSprites == null)
            attachmentSprites = new HashMap<Class<? extends NodeAttachment>, Sprite>() {
                {
                    put(Photoreceptor.class, ImageUtils.loadSprite("cell/nodes/photoreceptor/base.png"));
//                    put(AdhesionReceptor.class, ImageUtils.loadSprite("cell/nodes/binding_node.png"));
                    put(PhagocyticReceptor.class, ImageUtils.loadSprite("cell/nodes/phagoreceptor.png"));
                    put(PlantOnlyPhagocyticReceptor.class, ImageUtils.loadSprite("cell/nodes/phagoreceptor.png"));
                    put(MeatOnlyPhagocyticReceptor.class, ImageUtils.loadSprite("cell/nodes/meat_phagoreceptor.png"));
                }
            };
        return attachmentSprites;
    }

    protected SurfaceNode node;
    private final Optional<Class<? extends NodeAttachment>> attachmentClass;

    public NodeRenderer(SurfaceNode node) {
        this.node = node;
        this.attachmentClass = Optional.ofNullable(node.getAttachment())
                .map(NodeAttachment::getClass);
    }

    public SurfaceNode getNode() {
        return node;
    }

    public Sprite getSprite(float delta) {
        return getAttachmentSprites().get(node.getAttachment().getClass());
    }

    public boolean skipRenderCondition() {
        return !node.exists() || node.getAttachment() == null || node.getCell() == null
                || !getAttachmentSprites().containsKey(node.getAttachment().getClass())
                || (node.getAttachment() instanceof AdhesionReceptor
                && ((AdhesionReceptor) node.getAttachment()).isBound());
    }

    public void render(float delta, SpriteBatch batch) {
        if (skipRenderCondition())
            return;

        Sprite sprite = getSprite(delta);
        Cell cell = node.getCell();
        sprite.setColor(cell.getColor());
        drawAtNode(batch, sprite);
    }

    public void drawAtNode(SpriteBatch batch, Sprite sprite) {
        float scale = 0.4f;
        if (node.hasAttachment())
            scale *= node.getAttachmentConstructionProgress();
        drawAtNode(batch, sprite, scale);
    }

    public void drawAtNode(SpriteBatch batch, Sprite sprite, float scale) {
        Cell cell = node.getCell();
        ImageUtils.drawOnCircumference(
                batch, sprite, cell.getPos(),
                0.98f * cell.getRadius(),
                node.getAngle() + cell.getParticle().getAngle(),
                scale * cell.getRadius());
    }

    public boolean isStale() {
        if (node.getAttachment() == null)
            return true;

        Class<?> currAttachmentClass = node.getAttachment().getClass();
        return node.getCell().isDead() || attachmentClass
                .map(cls -> !cls.equals(currAttachmentClass))
                .orElse(currAttachmentClass != null);
    }

    public void renderDebug(ShapeRenderer sr) {}

    public static void dispose() {
        if (nodeEmptySprite != null)
            nodeEmptySprite.getTexture().dispose();
        if (attachmentSprites != null)
            for (Sprite attachmentSprite : attachmentSprites.values())
                attachmentSprite.getTexture().dispose();
        nodeEmptySprite = null;
        attachmentSprites = null;
    }
}
