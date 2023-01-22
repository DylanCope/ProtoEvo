package com.protoevo.biology.nodes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.core.Shape;
import com.protoevo.core.Particle;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.utils.Utils;

public class LightSensitiveAttachment extends NodeAttachment {
    private final Vector2[] ray = new Vector2[]{new Vector2(), new Vector2()};
    private final Shape.Collision[] collisions =
            new Shape.Collision[]{new Shape.Collision(), new Shape.Collision()};
    private final Vector2 tmp = new Vector2(), tmp2 = new Vector2();
    private final Vector2 attachmentRelPos = new Vector2();
    private float interactionRange = 0;
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
    public void update(float delta, float[] input, float[] output) {
        interactionRange = getInteractionRange();
        attachmentRelPos.set(node.getRelativePos());
        castRays();
        output[0] = colour.r * 2 - 1f;
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

        ray[1].set(attachmentRelPos)
                .setLength(interactionRange)
                .rotateRad(t)
                .add(node.getCell().getPos());

        rayIdx++;
        return ray;
    }

    private void castRays() {
        ray[0].set(attachmentRelPos)
                .add(node.getCell().getPos());

        for (reset(); rayIdx < nRays; nextRay()) {
            Cell cell = node.getCell();
            for (Object o : cell.getInteractionQueue())
                if (o instanceof Shape)
                    handleCollidable((Shape) o);
        }

        colour.set(r / (nRays + 1), g / (nRays + 1), b / (nRays + 1), 1);
        reset();
    }

	public boolean cullFromRayCasting(Shape o) {
		if (o instanceof Particle) {
			Vector2 otherPos = ((Particle) o).getPos();
            Vector2 myPos = node.getCell().getPos();
			Vector2 dx = tmp.set(otherPos).sub(myPos).nor();
            Vector2 dir = tmp2.set(attachmentRelPos).add(myPos).nor();
			return dx.dot(dir) < Math.cos(fov / 2f);
		}
		return false;
	}

    public Shape.Collision[] handleCollidable(Shape o) {
        collisions[0].didCollide = false;
        collisions[1].didCollide = false;
        boolean anyCollision = o.rayCollisions(ray, collisions);
        if (!anyCollision)
            return collisions;

        float sqLen = Float.MAX_VALUE;
        for (Shape.Collision collision : collisions)
            if (collision.didCollide)
                sqLen = Math.min(sqLen, collision.point.dst2(ray[0]));

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
        r = 1; g = 1; b = 1;
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
    public float getInteractionRange() {
        return Utils.linearRemap(
                node.getCell().getRadius(),
                ProtozoaSettings.minProtozoanBirthRadius, SimulationSettings.maxParticleRadius,
                node.getCell().getRadius() * 3f, ProtozoaSettings.protozoaLightRange);
    }
}
