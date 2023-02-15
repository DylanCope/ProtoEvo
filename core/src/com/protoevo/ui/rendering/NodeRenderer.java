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

public class NodeRenderer {
    protected static final Sprite nodeEmptySprite = ImageUtils.loadSprite("cell/nodes/node_empty.png");
    private static final Map<Class<? extends NodeAttachment>, Sprite> attachmentSprites =
            new HashMap<Class<? extends NodeAttachment>, Sprite>() {
                {
                    put(Spike.class, ImageUtils.loadSprite("cell/nodes/spike_node.png"));
                    put(Photoreceptor.class, ImageUtils.loadSprite("cell/nodes/photoreceptor/base.png"));
                    put(AdhesionReceptor.class, ImageUtils.loadSprite("cell/nodes/binding_node.png"));
                    put(PhagocyticReceptor.class, ImageUtils.loadSprite("cell/nodes/phagoreceptor.png"));
                }
            } ;

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
        return Optional.ofNullable(node.getAttachment())
                .map(NodeAttachment::getClass)
                .map(attachmentSprites::get)
                .orElse(nodeEmptySprite);
    }

    public void render(float delta, SpriteBatch batch) {
        if (!node.exists())
            return;

        if (node.getAttachment() instanceof AdhesionReceptor
                && ((AdhesionReceptor) node.getAttachment()).getOtherNode().isPresent())
            return;

        Cell cell = node.getCell();
        Sprite sprite = getSprite(delta);
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
                node.getAngle() + cell.getAngle(),
                scale * cell.getRadius());
    }

    public boolean isStale() {
        if (node.getAttachment() == null)
            return true;

        return node.getCell().isDead() || attachmentClass
                .map(cls -> !cls.equals(node.getAttachment().getClass()))
                .orElse(false);
    }

    public void renderDebug(ShapeRenderer sr) {}

    public void dispose() {

    }
}
