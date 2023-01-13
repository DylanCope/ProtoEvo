package com.protoevo.env;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.core.Collidable;
import com.protoevo.core.Simulation;
import com.protoevo.utils.Geometry;

import java.io.Serializable;
import java.util.ArrayList;

public class Rock implements Serializable, Collidable {
    public static final long serialVersionUID = 1L;

    private final Vector2[] points;
    private final Vector2[][] edges;
    private final boolean[] edgeAttachStates;
    private final Vector2 centre;
    private final Vector2[] normals;
    private final Vector2[] boundingBox;
    private final Color colour;

    public Rock(Vector2 p1, Vector2 p2, Vector2 p3) {
        points = new Vector2[]{p1, p2, p3};
        edges = new Vector2[][]{
                {points[0], points[1]},
                {points[1], points[2]},
                {points[0], points[2]}
        };
        edgeAttachStates = new boolean[]{false, false, false};
        centre = computeCentre();
        normals = computeNormals();
        colour = randomRockColour();
        boundingBox = computeBounds();
    }

    private Vector2 computeCentre() {
        Vector2 c = new Vector2(0, 0);
        for (Vector2 p : points)
            c.add(p);
        return c.scl(1f / points.length);
    }

    private Vector2[] computeBounds() {
        float minX = Math.min(points[0].x, Math.min(points[1].x, points[2].x));
        float minY = Math.min(points[0].y, Math.min(points[1].y, points[2].y));
        float maxX = Math.max(points[0].x, Math.max(points[1].x, points[2].x));
        float maxY = Math.max(points[0].y, Math.max(points[1].y, points[2].y));
        return new Vector2[]{new Vector2(minX, minY), new Vector2(maxX, maxY)};
    }

    public Vector2[] getBoundingBox() {
        return boundingBox;
    }

    public Vector2[][] getEdges() {
        return edges;
    }

    public boolean isEdgeAttached(int edgeIdx) {
        return edgeAttachStates[edgeIdx];
    }

    public void setEdgeAttached(int edgeIdx) {
        edgeAttachStates[edgeIdx] = true;
    }

    private Vector2[] computeNormals() {
        Vector2[] normals = new Vector2[3];
        Vector2[][] edges = getEdges();
        for (int i = 0; i < edges.length; i++)
            normals[i] = normal(edges[i][0], edges[i][1]);
        return normals;
    }

    private Vector2 normal(Vector2 p1, Vector2 p2) {
        Vector2 n = Geometry.perp(p1.cpy().sub(p2)).nor();
        if (n.dot(p1.cpy().sub(centre)) < 0)
            n.scl(-1);
        return n;
    }

    public Vector2[] getPoints() {
        return points;
    }

    public Vector2[] getEdge(int i) {
        return getEdges()[i];
    }

    public Vector2[] getNormals() {
        return normals;
    }

    private float sign(Vector2 p1, Vector2 p2, Vector2 p3) {
        return (p1.x - p3.x) * (p2.y - p3.y)
                - (p2.x - p3.x) * (p1.y - p3.y);
    }

    public boolean pointInside(Vector2 x) {
        float d1 = sign(x, points[0], points[1]);
        float d2 = sign(x, points[1], points[2]);
        float d3 = sign(x, points[2], points[0]);

        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

        return !(hasNeg && hasPos);
    }

    @Override
    public boolean rayIntersects(Vector2 start, Vector2 end) {
        return false;
    }

    @Override
    public Vector2[] rayCollisions(Vector2 start, Vector2 end) {
        Vector2[] ray = new Vector2[]{start, end};
        Vector2 dirRay = ray[1].sub(ray[0]);
        ArrayList<Vector2> collisions = new ArrayList<>(edges.length * 2);
        for (int i = 0; i < edges.length; i++) {
            if (isEdgeAttached(i))
                continue;

            Vector2[] edge = edges[i];
            Vector2 dirEdge = edge[1].sub(edge[0]);
            float[] coefs = edgesIntersectCoef(ray[0], dirRay, edge[0], dirEdge);
            if ((coefs != null) && edgeIntersectCondition(coefs))
                collisions.add(ray[0].cpy().add(dirRay.cpy().scl(coefs[0])));

        }
        return collisions.toArray(new Vector2[0]);
    }

    public boolean intersectsWith(Rock otherRock) {
        for (Vector2[] e1 : otherRock.getEdges())
            for (Vector2[] e2 : getEdges())
                if (edgesIntersect(e1, e2))
                    return true;
        return false;
    }

    public boolean intersectsWith(Vector2 circlePos, float radius) {
        for (Vector2[] e : getEdges())
            if (Geometry.doesLineIntersectCircle(e, circlePos, radius))
                return true;
        return false;
    }

    public static float[] edgesIntersectCoef(Vector2 start1, Vector2 dir1, Vector2 start2, Vector2 dir2) {

        float[][] coefs = new float[][]{
                {dir1.len2(), -dir1.dot(dir2)},
                {-dir2.dot(dir1), dir2.len2()}
        };

        float[] consts = new float[]{
                start2.dot(dir1) - start1.dot(dir1),
                start1.dot(dir2) - start2.dot(dir2),
        };

        float det = coefs[0][0] * coefs[1][1] - coefs[1][0] * coefs[0][1];

        if (det == 0)
            return null;

        float t1 = (consts[0]*coefs[1][1] - consts[1]*coefs[0][1]) / det;
        float t2 = (-consts[0]*coefs[1][0] + consts[1]*coefs[0][0]) / det;

        return new float[]{t1, t2};
    }

    public static boolean edgeIntersectCondition(float[] coefs) {
        float t1 = coefs[0], t2 = coefs[1];
        return 0f < t1 && t1 < 1f && 0f < t2 && t2 < 1f;
    }

    public static boolean edgesIntersect(Vector2 start1, Vector2 dir1, Vector2 start2, Vector2 dir2) {
        float[] coefs = edgesIntersectCoef(start1, dir1, start2, dir2);
        if (coefs == null)
            return false;
        return edgeIntersectCondition(coefs);
    }

    public static boolean edgesIntersect(Vector2[] e1, Vector2[] e2) {
        Vector2 dir1 = e1[1].cpy().sub(e1[0]);
        Vector2 dir2 = e2[1].cpy().sub(e2[0]);
        return edgesIntersect(e1[0], dir1, e2[0], dir2);
    }

    public Color getColor() {
        return colour;
    }

    public static Color randomRockColour() {
        float darkener = 0.6f;
        int tone = 80 + Simulation.RANDOM.nextInt(20);
        int yellowing = Simulation.RANDOM.nextInt(20);
        return new Color(
                darkener * (tone + yellowing) / 255.f,
                darkener * (tone + yellowing) / 255.f,
                darkener * tone / 255.f, 1);
    }

    public boolean allEdgesAttached() {
        return isEdgeAttached(0) && isEdgeAttached(1) && isEdgeAttached(2);
    }

    public Vector2 getCentre() {
        return centre;
    }
}
