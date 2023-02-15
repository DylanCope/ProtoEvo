package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Shape;
import com.protoevo.core.Particle;
import com.protoevo.settings.ProtozoaSettings;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.utils.Colour;
import com.protoevo.utils.Utils;

import java.io.Serial;
import java.io.Serializable;

public class Photoreceptor extends NodeAttachment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final Vector2[] ray = new Vector2[]{new Vector2(), new Vector2()};
    private final Shape.Collision[] collisions =
            new Shape.Collision[]{new Shape.Collision(), new Shape.Collision()};
    private final Vector2 tmp = new Vector2(), tmp2 = new Vector2();
    private final Vector2 attachmentRelPos = new Vector2();
    private float interactionRange = 0;
    private final Colour colour = new Colour();
    private float r, g, b;
    private int rayIdx;
    private float minSqLen;
    public static final int nRays = 8;
    public static final float fov = (float) (Math.PI / 2.);

    public Photoreceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        interactionRange = getInteractionRange();
        attachmentRelPos.set(node.getRelativePos());
        castRays();
        output[0] = colour.r * 2 - 1f;
        output[1] = colour.g * 2 - 1f;
        output[2] = colour.b * 2 - 1f;
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
            float w = getConstructionProgress() * computeColourFalloffWeight();
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

    public Colour getColour() {
        return colour;
    }

    @Override
    public float getInteractionRange() {
        if (node.getCell() == null)
            return 0;
        return Utils.linearRemap(
                node.getCell().getRadius(),
                ProtozoaSettings.minProtozoanBirthRadius, SimulationSettings.maxParticleRadius,
                node.getCell().getRadius() * 5f, ProtozoaSettings.protozoaLightRange);
    }

    @Override
    public String getName() {
        return "Photoreceptor";
    }

    @Override
    public String getInputMeaning(int index) {
        return null;  // no inputs
    }

    @Override
    public String getOutputMeaning(int index) {
        if (index == 0)
            return "R";
        else if (index == 1)
            return "G";
        else if (index == 2)
            return "B";
        return null;
    }
}
