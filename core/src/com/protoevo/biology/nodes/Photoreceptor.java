package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.physics.Coloured;
import com.protoevo.physics.Shape;
import com.protoevo.physics.box2d.Box2DParticle;
import com.protoevo.utils.Colour;
import com.protoevo.utils.Utils;


import java.io.Serializable;

import static com.protoevo.biology.nodes.Photoreceptor.ColourSensitivity.*;

public class Photoreceptor extends NodeAttachment implements Serializable {

    public enum ColourSensitivity {
        RGB, R, G, B, GrayLevel
    }

    private static final long serialVersionUID = 1L;
    private final Vector2[] ray = new Vector2[]{new Vector2(), new Vector2()};
    private final Shape.Intersection[] intersections =
            new Shape.Intersection[]{new Shape.Intersection(), new Shape.Intersection()};
    private final Vector2 tmp = new Vector2(), tmp2 = new Vector2();
    private final Vector2 attachmentRelPos = new Vector2();
    private float interactionRange = 0;
    private final Colour colour = new Colour();
    private float r, g, b;
    private ColourSensitivity colourSensitivity = RGB;
    private int rayIdx;
    private float minSqLen;
    private static final float maxFoV = (float) (Math.PI / 2.);
    private static final float radiansPerRay = maxFoV / 8f; // max of 8 rays
    public int nRays = 8;
    public float fov = maxFoV;

    public Photoreceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        fov = Utils.clampedLinearRemap(input[0], -1f, 1f, 0.1f * maxFoV, maxFoV);
        nRays = Math.max(1, Math.round(fov / radiansPerRay));

        interactionRange = getInteractionRange();
        attachmentRelPos.set(node.getRelativePos());
        castRays();

        if (output.length == 3) {
            output[0] = colour.r;
            output[1] = colour.g;
            output[2] = colour.b;
        }
        else if (output.length == 1) {
            if (colourSensitivity == RGB)
                colourSensitivity = GrayLevel;

            switch (colourSensitivity) {
                case R:
                    output[0] = colour.r;
                    break;
                case G:
                    output[0] = colour.g;
                    break;
                case B:
                    output[0] = colour.b;
            }
        }
        else {
            throw new RuntimeException("Do not know how to deal with requested output of dim: " + output.length);
        }
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
            for (Object o : cell.getParticle().getInteractionQueue())
                if (o instanceof Shape && o instanceof Coloured)
                    computeIntersections((Shape) o);
        }


        switch (colourSensitivity) {
            case R:
                g = 0; b = 0;
                break;
            case G:
                b = 0; r = 0;
                break;
            case B:
                r = 0; g = 0;
                break;
            case GrayLevel:
                float meanRGB = (r + g + b) / 3f;
                r = meanRGB; g = meanRGB; b = meanRGB;
        }

        colour.set(r / (nRays + 1), g / (nRays + 1), b / (nRays + 1), 1);
        reset();
    }

	public boolean cullFromRayCasting(Shape o) {
		if (o instanceof Box2DParticle) {
			Vector2 otherPos = ((Box2DParticle) o).getPos();
            Vector2 myPos = node.getCell().getPos();
			Vector2 dx = tmp.set(otherPos).sub(myPos).nor();
            Vector2 dir = tmp2.set(attachmentRelPos).add(myPos).nor();
			return dx.dot(dir) < Math.cos(fov / 2f);
		}
		return false;
	}

    public Shape.Intersection[] computeIntersections(Shape o) {
        intersections[0].didCollide = false;
        intersections[1].didCollide = false;
        boolean anyCollision = o.rayCollisions(ray, intersections);
        if (!anyCollision)
            return intersections;

        float sqLen = Float.MAX_VALUE;
        Shape.Intersection collisionIntersection = null;
        for (Shape.Intersection intersection : intersections) {
            if (intersection.didCollide) {
                sqLen = Math.min(sqLen, intersection.point.dst2(ray[0]));
                collisionIntersection = intersection;
            }
        }

        if (sqLen < minSqLen && collisionIntersection != null) {
            minSqLen = sqLen;
            float light = node.getCell().getLightAt(collisionIntersection.point);
            float w = light * getConstructionProgress() * computeColourFalloffWeight();
            Coloured coloured = (Coloured) o;
            r += coloured.getColour().r * w;
            g += coloured.getColour().g * w;
            b += coloured.getColour().b * w;
        }

        return intersections;
    }

    public void reset() {
        r = 1; g = 1; b = 1;
        rayIdx = 0;
    }

    public float computeColourFalloffWeight() {
        float ir2 = getInteractionRange() * getInteractionRange();
        return Utils.clampedLinearRemap(minSqLen,0.5f * ir2, ir2,1, 0);
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
        return Utils.clampedLinearRemap(
                node.getCell().getRadius(),
                Environment.settings.protozoa.minBirthRadius.get(), Environment.settings.maxParticleRadius.get(),
                node.getCell().getRadius() * 5f, Environment.settings.protozoa.maxLightRange.get());
    }

    @Override
    public String getName() {
        return "Photoreceptor";
    }

    @Override
    public String getInputMeaning(int index) {
        if (index == 0)
            return "Focus";
        return null;
    }

    @Override
    public String getOutputMeaning(int index) {
        if (colourSensitivity == RGB) {
            if (index == 0)
                return "R";
            else if (index == 1)
                return "G";
            else if (index == 2)
                return "B";
        }
        else if (index == 0) {
            return colourSensitivity.name();
        }
        return null;
    }

    @Override
    public void addStats(Statistics stats) {
        stats.putPercentage("Input: Red Light", colour.r);
        stats.putPercentage("Input: Green Light", colour.g);
        stats.putPercentage("Input: Blue Light", colour.b);
        stats.putDistance("Interaction Range", getInteractionRange());
        stats.put("FoV", fov, Statistics.ComplexUnit.ANGLE);
    }

    public int getNRays() {
        return nRays;
    }
}
