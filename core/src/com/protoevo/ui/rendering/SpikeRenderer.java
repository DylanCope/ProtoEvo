package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.nodes.NodeAttachment;
import com.protoevo.biology.nodes.Photoreceptor;
import com.protoevo.biology.nodes.Spike;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.core.Shape;
import com.protoevo.utils.ImageUtils;

public class SpikeRenderer extends NodeRenderer {
    private static Sprite spikeSprite = null;

    public static Sprite getSpikeSprite() {
        if (spikeSprite == null) {
            spikeSprite = ImageUtils.loadSprite("cell/nodes/spike_node.png");
        }
        return spikeSprite;
    }

    public static void disposeStaticSprite() {
        if (spikeSprite != null) {
            spikeSprite.getTexture().dispose();
            spikeSprite = null;
        }
    }

    public SpikeRenderer(SurfaceNode node) {
        super(node);
    }

    @Override
    public Sprite getSprite(float delta) {
        return getNodeEmptySprite();
    }

    @Override
    public void render(float delta, SpriteBatch batch) {
        Cell cell = node.getCell();
        NodeAttachment attachment = node.getAttachment();
        if (!(attachment instanceof Spike))
            return;

        Spike spike = (Spike) attachment;
        float spikeLen = spike.getSpikeLength();
        Sprite sprite = getSpikeSprite();
        float width = spikeLen * sprite.getWidth() / sprite.getHeight();
        sprite.setColor(cell.getColor());
        ImageUtils.drawOnCircumference(
                batch, sprite, cell.getPos(),
                0.98f * cell.getRadius() - (1 - spike.getSpikeExtension()) * spikeLen,
                node.getAngle() + cell.getAngle(),
                width);
    }

    @Override
    public void renderDebug(ShapeRenderer sr) {
        if (node.getAttachment() == null || node.getCell() == null)
            return;

        Cell cell = node.getCell();

        Vector2 spikePoint = ((Spike) node.getAttachment()).getSpikePoint();
        sr.setColor(1, 0, 0, 1);
        sr.line(node.getWorldPosition(), spikePoint);

        for (Object toInteract : cell.getInteractionQueue()) {
            if (toInteract instanceof Cell) {
                Cell other = (Cell) toInteract;
                if (other.isPointInside(spikePoint)) {
                    sr.circle(other.getPos().x, other.getPos().y, 1.15f * other.getRadius());
                }
            }
        }

    }
}
