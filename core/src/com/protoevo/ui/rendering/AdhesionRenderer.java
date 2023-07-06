package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.nodes.AdhesionReceptor;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.physics.Joining;

import java.util.Optional;

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
            Optional<Joining> joining = node.getCell().getEnv()
                    .flatMap(e -> e.getJointsManager().getJoining(adhesionReceptor.getJoiningID()));
            if (adhesionReceptor.isBound() && joining.isPresent()) {
                Optional<Vector2> anchorA = joining.flatMap(Joining::getAnchorA);
                Optional<Vector2> anchorB = joining.flatMap(Joining::getAnchorB);
                sr.setColor(0, 1, 0, 1);
                anchorA.ifPresent(anchor -> sr.circle(anchor.x, anchor.y, node.getCell().getRadius() / 10f));
                anchorB.ifPresent(anchor -> sr.circle(anchor.x, anchor.y, node.getCell().getRadius() / 10f));
                if (anchorA.isPresent() && anchorB.isPresent())
                    sr.line(anchorA.get(), anchorB.get());
            }
        }
    }
}
