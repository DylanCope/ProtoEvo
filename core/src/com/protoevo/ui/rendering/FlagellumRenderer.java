package com.protoevo.ui.rendering;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.nodes.Flagellum;
import com.protoevo.biology.nodes.SurfaceNode;
import com.protoevo.core.Simulation;
import com.protoevo.settings.ProtozoaSettings;
import com.protoevo.utils.ImageUtils;
import com.protoevo.utils.Utils;

public class FlagellumRenderer extends NodeRenderer {

    private static Sprite[] animationFrames;

    public static Sprite[] getAnimationFrames() {
        if (animationFrames == null) {
            animationFrames = ImageUtils.loadSpriteAnimationFrames("cell/nodes/flagella/");
        }
        return animationFrames;
    }

    public static void disposeAnimation() {
        if (animationFrames != null) {
            for (Sprite frame : animationFrames)
                frame.getTexture().dispose();
            animationFrames = null;
        }
    }

    private float animationTime;
    private float animationSpeed = 3f;

    public FlagellumRenderer(SurfaceNode node) {
        super(node);
    }

    @Override
    public Sprite getSprite(float delta) {
        Flagellum attachment = (Flagellum) node.getAttachment();
        if (attachment == null)
            throw new RuntimeException("Expected flagellum attachment.");

        Sprite[] frames = getAnimationFrames();
        if (!Simulation.isPaused()) {
            float thrust = attachment.getThrustVector().len();
            if (thrust > 0) {
                float p = Utils.linearRemap(thrust,
                        0, ProtozoaSettings.maxFlagellumThrust,
                        0.5f, 1f);
                animationTime += animationSpeed * delta * p;
            }
//            if (animationTime <= 0 || animationTime >= 1)
//                animationSpeed *= -1;
            if (animationTime >= 1)
                animationTime = 0;
        }

        int idx = (int) (frames.length * animationTime);
        return frames[MathUtils.clamp(idx, 0,frames.length - 1)];
    }

    @Override
    public void render(float delta, SpriteBatch batch) {
        if (!node.exists())
            return;

        Cell cell = node.getCell();
        Sprite sprite = getSprite(delta);
        sprite.setColor(cell.getColor());
        drawAtNode(batch, sprite, 0.6f * node.getAttachmentConstructionProgress());
    }

    @Override
    public void renderDebug(ShapeRenderer sr) {
        if (node.getAttachment() == null)
            return;
        Cell cell = node.getCell();
        Vector2 pos = cell.getPos();

        Flagellum attachment = (Flagellum) node.getAttachment();
        float maxThrust = ProtozoaSettings.maxFlagellumThrust;
        Vector2 thrust = attachment.getThrustVector().cpy().setLength(cell.getRadius()*1.5f);
        float mag = Utils.linearRemap(thrust.len(), 0, maxThrust, 0, 1.5f);
        sr.setColor(0, 1, 0, 1);
        Vector2 v = thrust.cpy().setLength(mag * cell.getRadius());
        sr.line(pos.x, pos.y, pos.x + v.x, pos.y + v.y);
    }
}
