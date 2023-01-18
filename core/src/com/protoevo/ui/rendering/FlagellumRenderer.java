package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
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
    private final float animationSpeed = 3f;

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
            float thrust = attachment.getThrustVector().len();
            if (thrust > 0) {
                float p = Utils.linearRemap(thrust,
                        0, ProtozoaSettings.maxProtozoaThrust,
                        0.5f, 1f);
                animationTime += animationSpeed * delta * p;
            }
            if (animationTime >= 1)
                animationTime %= 1;
        }

        return frames[Math.min(idx, frames.length - 1)];
    }

    @Override
    public void renderDebug(ShapeRenderer sr) {
        if (!node.getAttachment().isPresent())
            return;
        Cell cell = node.getCell();
        Vector2 pos = cell.getPos();

        FlagellumAttachment attachment = (FlagellumAttachment) node.getAttachment().get();
        float maxThrust = ProtozoaSettings.maxProtozoaThrust;
        Vector2 thrust = attachment.getThrustVector().cpy().setLength(cell.getRadius()*1.5f);
        float mag = Utils.linearRemap(thrust.len(), 0, maxThrust, 0, 1.5f);
        sr.setColor(0, 1, 0, 1);
        Vector2 v = thrust.cpy().setLength(mag * cell.getRadius());
        sr.line(pos.x, pos.y, pos.x + v.x, pos.y + v.y);
    }
}
