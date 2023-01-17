package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.nodes.LightSensitiveAttachment;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.core.Shape;
import com.protoevo.utils.ImageUtils;

public class LightSensitiveNodeRenderer extends NodeRenderer {
    protected static final Sprite lightSensitiveSprite = ImageUtils.loadSprite("cell/light_sensitive_node.png");

    public LightSensitiveNodeRenderer(SurfaceNode node) {
        super(node);
    }

    @Override
    public Sprite getSprite(float delta) {
        return nodeEmptySprite;
    }

    @Override
    public void render(float delta, SpriteBatch batch) {
        super.render(delta, batch);
        if (node.getAttachment().isPresent() &&
                node.getAttachment().get() instanceof LightSensitiveAttachment) {
            LightSensitiveAttachment attachment = (LightSensitiveAttachment) node.getAttachment().get();
            lightSensitiveSprite.setColor(attachment.getColour());
            renderRotatedNode(lightSensitiveSprite, batch);
        }
    }

    private void renderDebugRay(ShapeRenderer sr, Vector2[] ray, LightSensitiveAttachment attachment) {
        Cell cell = node.getCell();
        sr.line(ray[0], ray[1]);
        for (Object o : cell.getInteractionQueue()) {
            if (o instanceof Shape) {
                Shape.Collision[] collisions = attachment.handleCollidable((Shape) o);
                for (Shape.Collision collision : collisions) {
                    if (collision.didCollide)
                        sr.circle(collision.point.x, collision.point.y,
                                cell.getRadius() / 15f, 15);
                }
            }
        }
    }

    @Override
    public void renderDebug(ShapeRenderer sr) {
        if (!node.getAttachment().isPresent())
            return;

        LightSensitiveAttachment attachment = (LightSensitiveAttachment) node.getAttachment().get();
        sr.setColor(1, 0, 0, 1);

        SurfaceNode node = attachment.getNode();
        attachment.reset();
        for (int rayIdx = 0; rayIdx < LightSensitiveAttachment.nRays; rayIdx++) {
            Vector2[] ray = attachment.nextRay();
            renderDebugRay(sr, ray, attachment);
        }
    }
}
