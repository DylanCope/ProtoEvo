package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.protoevo.biology.nodes.LightSensitiveAttachment;
import com.protoevo.biology.nodes.SurfaceNode;
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
}
