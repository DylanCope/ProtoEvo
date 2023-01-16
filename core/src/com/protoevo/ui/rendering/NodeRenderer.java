package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.nodes.*;
import com.protoevo.utils.ImageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NodeRenderer {
    protected static final Sprite nodeEmptySprite = ImageUtils.loadSprite("cell/surface_node_empty.png");
    private static final Map<Class<? extends NodeAttachment>, Sprite> attachmentSprites =
            new HashMap<Class<? extends NodeAttachment>, Sprite>(){
        {
            put(SpikeAttachment.class, ImageUtils.loadSprite("cell/spike/spike_large.png"));
            put(LightSensitiveAttachment.class, ImageUtils.loadSprite("cell/light_sensitive_node.png"));
        }
    };

    protected SurfaceNode node;
    private final Optional<Class<? extends NodeAttachment>> attachmentClass;

    public NodeRenderer(SurfaceNode node) {
        this.node = node;
        this.attachmentClass = node.getAttachment().map(NodeAttachment::getClass);
    }

    public SurfaceNode getNode() {
        return node;
    }

    public Sprite getSprite(float delta) {
        return node.getAttachment()
                .map(NodeAttachment::getClass)
                .map(attachmentSprites::get)
                .orElse(nodeEmptySprite);
    }

    public void render(float delta, SpriteBatch batch) {
        Cell cell = node.getCell();
        Sprite sprite = getSprite(delta);
        sprite.setColor(cell.getColor());
        renderRotatedNode(sprite, batch);
    }

    public void renderRotatedNode(Sprite nodeSprite, SpriteBatch batch) {
        Cell cell = node.getCell();
        Vector2 pos = cell.getPos();
        float cellAngle = cell.getAngle();
        float size = 2 * cell.getRadius();
        nodeSprite.setPosition(pos.x - size, pos.y - size);
        nodeSprite.setSize(2*size, 2*size);
        nodeSprite.setOriginCenter();
        float attachmentAngle = (float) Math.toDegrees(cellAngle + node.getAngle());
        nodeSprite.setRotation(attachmentAngle - 90);
        nodeSprite.draw(batch);
    }

    public boolean isStale() {
        return !node.getAttachment()
                .map(NodeAttachment::getClass)
                .equals(attachmentClass);
    }

    public void renderDebug(ShapeRenderer sr) {}
}
