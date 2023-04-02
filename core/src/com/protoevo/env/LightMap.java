package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.physics.Shape;
import com.protoevo.physics.SpatialHash;
import com.protoevo.utils.Geometry;
import com.protoevo.utils.Utils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;

public class LightMap implements Serializable {
    public static long serialVersionUID = 1L;

    public static void bakeRockShadows(LightMap lightMap, List<Rock> rocks) {
        float rayLen = Environment.settings.world.maxRockSize.get() * 3f;

        int resolution = (int) (lightMap.getFieldWidth() / rayLen);

        SpatialHash<Rock> rockSpatialHash = new SpatialHash<>(resolution, Environment.settings.world.radius.get());
        for (Rock rock : rocks) {
            rockSpatialHash.add(rock, rock.getBoundingBox());
        }

        collectLight(lightMap, rockSpatialHash, rayLen);
    }

    private static void collectLight(LightMap lightMap, SpatialHash<Rock> rockSpatialHash, float rayLen) {

        int lightSamples = 12;

        final Vector2[] ray = new Vector2[2];

        final Shape.Intersection[] intersections =
                new Shape.Intersection[]{new Shape.Intersection(), new Shape.Intersection()};

        IntStream.range(0, lightMap.width * lightMap.height).forEach(i -> {
            int x = i % lightMap.width;
            int y = i / lightMap.width;
            Vector2 cellPos = lightMap.toEnvironmentCoords(x, y);
            ray[0] = cellPos;

            float collisionValue = 0f;

            int hashIMin = rockSpatialHash.getChunkX(cellPos.x - rayLen);
            int hashIMax = rockSpatialHash.getChunkX(cellPos.x + rayLen);
            int hashJMin = rockSpatialHash.getChunkY(cellPos.y - rayLen);
            int hashJMax = rockSpatialHash.getChunkY(cellPos.y + rayLen);

            Set<Rock> checkRocks = new HashSet<>();

            for (int a = 0; a < lightSamples; a++) {
                float angle = (float) (a * Math.PI * 2 / lightSamples);
                Vector2 rayDir = Geometry.fromAngle(angle).scl(rayLen);
                ray[1] = cellPos.cpy().add(rayDir);
                float dist = rayLen;

                checkRocks.clear();

                for (int hashI = hashIMin; hashI <= hashIMax; hashI++) {
                    for (int hashJ = hashJMin; hashJ <= hashJMax; hashJ++) {
                        checkRocks.addAll(rockSpatialHash.getChunkContents(hashI, hashJ));
                    }
                }

                for (Rock rock : checkRocks) {
                    intersections[0].didCollide = false;
                    intersections[1].didCollide = false;
                    if (rock.rayCollisions(ray, intersections)) {
                        dist = Math.min(dist, getClosestDist(cellPos, intersections));
                    }
                }
                collisionValue += 1 - dist / rayLen;
            }

            collisionValue /= lightSamples;
            float t = 0.1f;
            float light = Utils.clampedLinearRemap(collisionValue, t, 1 - t, 1, 0);

            lightMap.setLight(x, y, light);
        });
    }

    private static float getClosestDist(Vector2 point, Shape.Intersection[] intersections) {
        float dist = Float.MAX_VALUE;
        if (intersections[0].didCollide)
            dist = Math.min(dist, point.dst(intersections[0].point));
        if (intersections[1].didCollide)
            dist = Math.min(dist, point.dst(intersections[1].point));
        return dist;
    }

    private final float[][] lightMap;
    private final int width, height;
    private float cellSizeX, cellSizeY;
    private float xMin, yMin, xMax, yMax;

    public LightMap(int width, int height, float radius) {
        this.width = width;
        this.height = height;
        this.xMin = -radius;
        this.yMin = -radius;
        this.xMax = radius;
        this.yMax = radius;
        this.cellSizeX = (xMax - xMin) / width;
        this.cellSizeY = (yMax - yMin) / height;

        lightMap = new float[width][height];
        for (float[] row : lightMap)
            Arrays.fill(row, 1f);
    }

    public float getLight(int x, int y) {
        return lightMap[x][y];
    }

    public void setLight(int x, int y, float light) {
        lightMap[x][y] = light;
    }

    public Vector2 toEnvironmentCoords(int i, int j) {
        float x = xMin + (0.5f + i) * cellSizeX;
        float y = yMin + (0.5f + j) * cellSizeY;
        return new Vector2(x, y);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getFieldWidth() {
        return xMax - xMin;
    }

    public float getFieldHeight() {
        return yMax - yMin;
    }

    public float getXMin() {
        return xMin;
    }

    public float getYMin() {
        return yMin;
    }

    public float getXMax() {
        return xMax;
    }

    public float getYMax() {
        return yMax;
    }
}
