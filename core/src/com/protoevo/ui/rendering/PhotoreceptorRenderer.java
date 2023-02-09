package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.nodes.Photoreceptor;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.core.Shape;
import com.protoevo.utils.ImageUtils;

public class PhotoreceptorRenderer extends NodeRenderer {
    private static final Sprite photoreceptorSprite =
            ImageUtils.loadSprite("cell/nodes/photoreceptor/light_patch.png");

    public PhotoreceptorRenderer(SurfaceNode node) {
        super(node);
    }

    @Override
    public Sprite getSprite(float delta) {
        return nodeEmptySprite;
    }

    @Override
    public void render(float delta, SpriteBatch batch) {
        super.render(delta, batch);
        if (node.getAttachment() != null &&
                node.getAttachment() instanceof Photoreceptor) {
            Photoreceptor attachment = (Photoreceptor) node.getAttachment();
            photoreceptorSprite.setColor(attachment.getColour().getColor());
            drawAtNode(batch, photoreceptorSprite);
        }
    }

    private void renderDebugRay(ShapeRenderer sr, Vector2[] ray, Photoreceptor attachment) {
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
        if (node.getAttachment() == null)
            return;

        Photoreceptor attachment = (Photoreceptor) node.getAttachment();
        sr.setColor(1, 0, 0, 1);

        SurfaceNode node = attachment.getNode();
        attachment.reset();
        for (int rayIdx = 0; rayIdx < Photoreceptor.nRays; rayIdx++) {
            Vector2[] ray = attachment.nextRay();
            renderDebugRay(sr, ray, attachment);
        }
    }
}
