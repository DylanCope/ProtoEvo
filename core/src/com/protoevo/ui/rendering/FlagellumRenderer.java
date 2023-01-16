package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.nodes.FlagellumAttachment;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.utils.ImageUtils;
import com.protoevo.utils.Utils;

public class FlagellumRenderer extends NodeRenderer {

    private static Sprite[] animationFrames;

    private static Sprite[] getAnimationFrames() {
        if (animationFrames == null) {
            animationFrames = ImageUtils.loadSpriteAnimationFrames("cell/flagella/");
        }
        return animationFrames;
    }

    private float animationTime;
    private float animationSpeed = 1.5f;

    public FlagellumRenderer(SurfaceNode node) {
        super(node);
    }

    @Override
    public Sprite getSprite(float delta) {
        FlagellumAttachment attachment = (FlagellumAttachment) node.getAttachment()
                .orElseThrow(() -> new RuntimeException("Expected flagellum attachment."));

        Sprite[] frames = getAnimationFrames();
        int idx = (int) (frames.length * animationTime);
        if (!Simulation.isPaused()) {
            Vector2 thrust = attachment.getThrustVector();
            float p = Utils.linearRemap(thrust.len(),
                    0, ProtozoaSettings.maxProtozoaThrust,
                    0.5f, 1.5f);
            animationTime += animationSpeed * delta * p;
            if (animationTime >= 1)
                animationTime %= 1;
        }

        return frames[Math.min(idx, frames.length - 1)];
    }
}
