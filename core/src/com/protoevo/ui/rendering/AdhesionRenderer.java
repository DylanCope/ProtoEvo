package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.nodes.AdhesionReceptor;
import com.protoevo.biology.nodes.Photoreceptor;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.env.JointsManager;
import com.protoevo.physics.Shape;
import com.protoevo.utils.ImageUtils;

public class AdhesionRenderer extends NodeRenderer {

    public AdhesionRenderer(SurfaceNode node) {
        super(node);
    }

    @Override
    public Sprite getSprite(float delta) {
        return getNodeEmptySprite();
    }

    @Override
    public void render(float delta, SpriteBatch batch) {
        Sprite sprite = CellTexture.getSprite();

        Cell cell = node.getCell();
        Vector2 pos = cell.getPos();
        float r = 1.1f * cell.getRadius();
        float x = pos.x - r;
        float y = pos.y - r;
        float size = r * 2;

        sprite.setColor(1f, 1f, 1f, 0.45f);
        sprite.setPosition(x, y);
        sprite.setSize(size, size);
        sprite.draw(batch);
    }

    @Override
    public void renderDebug(ShapeRenderer sr) {
        if (node.getAttachment() instanceof AdhesionReceptor) {
            AdhesionReceptor adhesionReceptor = (AdhesionReceptor) node.getAttachment();
            JointsManager.Joining joining = node.getCell().getEnv()
                    .getJointsManager().getJoining(adhesionReceptor.getJoiningID());
            if (adhesionReceptor.isBound() && joining != null) {
                Vector2 anchorA = joining.getAnchorA();
                Vector2 anchorB = joining.getAnchorB();
                sr.setColor(0, 1, 0, 1);
                sr.circle(anchorA.x, anchorA.y, node.getCell().getRadius() / 10f);
                sr.circle(anchorB.x, anchorB.y, node.getCell().getRadius() / 10f);
                sr.line(anchorA, anchorB);
            }
        }
    }
}
