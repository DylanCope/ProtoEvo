package com.protoevo.biology.nodes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.core.Collidable;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.utils.Utils;

public class LightSensitiveAttachment extends NodeAttachment {
    private final Vector2[] ray = new Vector2[]{new Vector2(), new Vector2()};
    private final Color colour = new Color();
    private float r, g, b;
    private int rayIdx;
    private float minSqLen;
    public static final int nRays = 8;
    public static final float fov = (float) (Math.PI / 2.);

    public LightSensitiveAttachment(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta) {
        castRays();
//        colour.mul(computeColourFalloffWeight());
    }

    public Vector2[] nextRay() {
        float t;
        float dt = fov / nRays;
        if (rayIdx == 0)
            t = 0;
        else if (rayIdx % 2 == 0)
            t = (float) (dt * Math.floor(rayIdx / 2f));
        else
            t = (float) (-dt * Math.floor(rayIdx / 2f));

        minSqLen = Float.MAX_VALUE;

        ray[0].set(node.getRelativePos())
                .add(node.getCell().getPos());

        ray[1].set(node.getRelativePos())
                .setLength(getInteractionRange())
                .rotateRad(t)
                .add(node.getCell().getPos());

        rayIdx++;
        return ray;
    }

    private void castRays() {
        r = 1; g = 1; b = 1;

        for (rayIdx = 0; rayIdx < nRays; nextRay()) {

            Cell cell = node.getCell();
            for (Object o : cell.getInteractionQueue())
                if (o instanceof Collidable)
                    handleCollidable((Collidable) o);
        }

        colour.set(r / (nRays + 1), g / (nRays + 1), b / (nRays + 1), 1);
        r = 1; g = 1; b = 1;
        rayIdx = 0;
    }

    public Vector2[] handleCollidable(Collidable o) {

        Vector2[] collisions = o.rayCollisions(ray[0], ray[1]);
        if (collisions == null || collisions.length == 0)
            return collisions;

        float sqLen = Float.MAX_VALUE;
        for (Vector2 collisionPoint : collisions)
            sqLen = Math.min(sqLen, collisionPoint.dst2(ray[0]));

        if (sqLen < minSqLen) {
            minSqLen = sqLen;
            float w = computeColourFalloffWeight();
            r += o.getColor().r * w;
            g += o.getColor().g * w;
            b += o.getColor().b * w;
        }

        return collisions;
    }

    public void reset() {
        rayIdx = 0;
    }

    public float computeColourFalloffWeight() {
        float ir2 = getInteractionRange() * getInteractionRange();
        return Utils.linearRemap(minSqLen,0.5f * ir2, ir2,1, 0);
    }

    public Vector2[] getRay() {
        return ray;
    }

    public Color getColour() {
        return colour;
    }

    @Override
    public void handleIO(float[] input, float[] output) {

    }

    @Override
    public float energyUsage() {
        return 0;
    }

    @Override
    public float getInteractionRange() {
        return Utils.linearRemap(
                node.getCell().getRadius(),
                ProtozoaSettings.minProtozoanBirthRadius, SimulationSettings.maxParticleRadius,
                node.getCell().getRadius() * 3f, ProtozoaSettings.protozoaLightRange);
    }
}
