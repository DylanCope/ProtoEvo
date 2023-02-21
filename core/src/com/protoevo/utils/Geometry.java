package com.protoevo.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

public class Geometry {

    public static final Vector2 ZERO = new Vector2(0, 0);

    public static Vector2 fromAngle(float angle) {
        return new Vector2(MathUtils.cos(angle), MathUtils.sin(angle));
    }

    public static float angle(Vector2 v) {
        float t = v.angleRad();
        if (t < 0)
            t += 2 * MathUtils.PI;
        return t;
    }

    public static float[] circleIntersectLineTs(Vector2 dir, Vector2 x, float r) {
        float a = dir.len2();
        float b = -2*dir.dot(x);
        float c = x.len2() - r*r;
        float disc = b*b - 4*a*c;
        if (disc < 0)
            return null;

        float t1 = (float) ((-b + Math.sqrt(disc)) / (2*a));
        float t2 = (float) ((-b - Math.sqrt(disc)) / (2*a));

        return new float[]{t1, t2};
    }

    public static boolean lineIntersectCondition(float[] ts) {
        if (ts == null)
            return false;
        float t1 = ts[0], t2 = ts[1];
        float eps = 1e-9f;
        return (eps < t1 && t1 < 1 - eps) || (eps < t2 && t2 < 1 - eps);
    }

    public static boolean doesLineIntersectCircle(Vector2[] line, Vector2 circlePos, float circleR) {
        Vector2 dir = line[1].cpy().sub(line[0]);
        Vector2 x = circlePos.cpy().sub(line[0]);
        float[] intersectionCoefs = circleIntersectLineTs(dir, x, circleR);
        return lineIntersectCondition(intersectionCoefs);
    }

    public static boolean isPointInsideCircle(Vector2 circlePos, float radius, Vector2 p) {
        return circlePos.dst2(p) <= radius * radius;
    }

    public static float getSphereVolume(float r) {
        return  (float) ((4 / 3) * Math.PI * r * r * r);
    }

    public static Vector2 perp(Vector2 v) {
        return new Vector2(-v.y, v.x);
    }

    public static Vector2 randomUnit() {
        return Geometry.fromAngle((float) (Math.random() * 2 * Math.PI));
    }

    public static Vector2 randomUnit(Random random) {
        return Geometry.fromAngle((float) (random.nextFloat() * 2 * Math.PI));
    }

    public static Vector2 randomVector(float length) {
        return Geometry.randomUnit().scl(length);
    }

    public static Vector2 randomVector(float length, Random random) {
        return Geometry.randomUnit(random).scl(length);
    }

    public static Vector2 randomPointInCircle(float circleR) {
        return Geometry.randomVector((float) Math.sqrt(Math.random() * circleR * circleR));
    }

    public static Vector2 randomPointInCircle(float circleR, Random random) {
        float length = (float) Math.sqrt(random.nextFloat() * circleR * circleR);
        return Geometry.randomVector(length, random);
    }

    public static float getCircleArea(float r) {
        return (float) (Math.PI * r * r);
    }

    public static double getCircleArea(double r) {
        return Math.PI * r * r;
    }

    public static boolean doCirclesCollide(Vector2 pos1, float r1, Vector2 pos2, float r2) {
        return pos1.dst2(pos2) <= (r1 + r2) * (r1 + r2);
    }

    public static float[][] equidistantPointsOnSphere(int n) {
        int d = 3;
        float[][] points = new float[n][d];
        float phi = (float) ((1 + Math.sqrt(5)) / 2);
        for (int i = 0; i < n; i++) {
            float t = (float) (2 * Math.PI * i / phi);
            points[i][0] = (float) Math.cos(t);
            points[i][1] = (float) Math.sin(t);
            points[i][2] = (float) (i / (float) n);
        }
        return points;
    }

    private static float positiveRootIntersect(float h, float r)
    // returns the positive root of intersection of line y = h with circle centered at the origin and radius r
    {
//        assert(r >= 0); // assume r is positive, leads to some simplifications in the formula below (can factor out r from the square root)
        return (h < r) ? (float) Math.sqrt(r * r - h * h) : 0f; // http://www.wolframalpha.com/input/?i=r+*+sin%28acos%28x+%2F+r%29%29+%3D+h
    }

    private static float indefIntCircleSeg(float x, float h, float r) // indefinite integral of circle segment
    {
        return (float) (.5f * (Math.sqrt(1 - x * x / (r * r)) * x * r + r * r * Math.asin(x / r) - 2 * h * x));
        // http://www.wolframalpha.com/input/?i=r+*+sin%28acos%28x+%2F+r%29%29+-+h
    }

    private static float boxAndCircleIntersectionOverlap(float x0, float x1, float h, float r)
    // area of intersection of an infinitely tall box with left edge at x0, right edge at x1, bottom edge at h
    // and top edge at infinity, with circle centered at the origin with radius r
    {
        if(x0 > x1) {
            float tmp = x0;
            x0 = x1;
            x1 = tmp;
        }
        float s = positiveRootIntersect(h, r);
        return indefIntCircleSeg(Math.max(-s, Math.min(s, x1)), h, r) - indefIntCircleSeg(Math.max(-s, Math.min(s, x0)), h, r); // integrate the area
    }

    private static float boxAndCircleIntersectionOverlap(float x0, float x1, float y0, float y1, float r)
    // area of the intersection of a finite box with a circle centered at the origin with radius r
    {
        if(y0 > y1) {
            float tmp = y0;
            y0 = y1;
            y1 = tmp;
        }

        if(y0 < 0) {
            if(y1 < 0)
                return boxAndCircleIntersectionOverlap(x0, x1, -y0, -y1, r);
            // the box is completely under, just flip it above and try again
            else
                return boxAndCircleIntersectionOverlap(x0, x1, 0, -y0, r)
                        + boxAndCircleIntersectionOverlap(x0, x1, 0, y1, r);
            // the box is both above and below, divide it to two boxes and go again
        } else {
//            assert(y1 >= 0); // y0 >= 0, which means that y1 >= 0 also (y1 >= y0) because of the swap at the beginning
            return boxAndCircleIntersectionOverlap(x0, x1, y0, r)
                    - boxAndCircleIntersectionOverlap(x0, x1, y1, r); // area of the lower box minus area of the higher box
        }
    }

    /**
     * Thank you "the swine":
     * https://stackoverflow.com/questions/622287/area-of-intersection-between-circle-and-rectangle
     * @param x0 the left edge of the box
     * @param x1 the right edge of the box
     * @param y0 the bottom edge of the box
     * @param y1 the top edge of the box
     * @param cx the x coordinate of the circle center
     * @param cy the y coordinate of the circle center
     * @param r the radius of the circle
     * @return the area of the intersection of the box and the circle
     */
    public static float boxAndCircleIntersectionOverlap(
            float x0, float x1, float y0, float y1, float cx, float cy, float r)
    // area of the intersection of a general box with a general circle
    {
        x0 -= cx; x1 -= cx;
        y0 -= cy; y1 -= cy;
        // get rid of the circle center

        return boxAndCircleIntersectionOverlap(x0, x1, y0, y1, r);
    }
}
