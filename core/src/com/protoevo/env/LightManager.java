package com.protoevo.env;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.maths.Functions;
import com.protoevo.maths.Shape;
import com.protoevo.physics.SpatialHash;
import com.protoevo.maths.Geometry;
import com.protoevo.utils.Perlin;

import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;

public class LightManager implements Serializable {
    public static long serialVersionUID = 1L;

    public static void bakeRockShadows(LightManager lightManager, List<Rock> rocks) {
        float rayLen = Environment.settings.worldgen.maxRockSize.get() * 5f;

        int resolution = (int) (lightManager.getFieldWidth() / rayLen);

        SpatialHash<Rock> rockSpatialHash = new SpatialHash<>(resolution, Environment.settings.worldgen.radius.get());
        for (Rock rock : rocks) {
            rockSpatialHash.add(rock, rock.getBoundingBox());
        }

        collectLight(lightManager, rockSpatialHash, rayLen);
    }

    private static void collectLight(LightManager lightManager, SpatialHash<Rock> rockSpatialHash, float rayLen) {

        int lightSamples = 16;

        final Vector2[] ray = new Vector2[2];

        final Shape.Intersection[] intersections =
                new Shape.Intersection[]{new Shape.Intersection(), new Shape.Intersection()};

        IntStream.range(0, lightManager.width * lightManager.height).forEach(i -> {
            int x = i % lightManager.width;
            int y = i / lightManager.width;
            Vector2 cellPos = lightManager.toEnvironmentCoords(x, y);
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
            float t = 0.25f;
            float light = Functions.clampedLinearRemap(collisionValue, t, 1 - t, 1, 0);

            lightManager.setCellLight(x, y, light);
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
    private final float cellSizeX, cellSizeY;
    private final float xMin, yMin, xMax, yMax;
    private final float radius;
    private float environmentLight = 1f;
    private TimeManager timeManager;

    public LightManager(int width, int height, float radius) {
        this.width = width;
        this.height = height;
        this.xMin = -radius;
        this.yMin = -radius;
        this.xMax = radius;
        this.yMax = radius;
        this.radius = radius;
        this.cellSizeX = (xMax - xMin) / width;
        this.cellSizeY = (yMax - yMin) / height;

        lightMap = new float[width][height];
        for (float[] row : lightMap)
            Arrays.fill(row, 1f);
    }

    public void setTimeManager(TimeManager timeManager) {
        this.timeManager = timeManager;
    }

    public void generateNoiseLight(float t) {
        float[][] noise = Perlin.generatePerlinNoise(width, height, 0);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                float light = Functions.clampedLinearRemap(
                    1 + 2 * noise[i][j], 0f, 1f, 0.2f, 1f);
                lightMap[i][j] *= light;

                Vector2 pos = toEnvironmentCoords(i, j);
                lightMap[i][j] = Functions.clampedLinearRemap(
                        pos.len(),
                        radius * 0.9f, radius,
                        lightMap[i][j], 1f
                );
            }
        }
    }

    public void update(float delta) {
        if (timeManager == null)
            return;

        if (!Environment.settings.env.dayNightCycleEnabled.get()) {
            environmentLight = 1f;
            return;
        }

        float t = timeManager.getTimeOfDayPercentage();

        float transition = Environment.settings.env.dayNightTransition.get();
        float night = Environment.settings.env.nightPercentage.get();
        float nightLightLevel = Environment.settings.env.nightLightLevel.get();

        if (t < 1 - 2*transition - night) {
            environmentLight = 1f;
        }
        else if (t <= 1 - transition - night) {
            environmentLight = Functions.clampedLinearRemap(
                    t, 1 - 2*transition - night, 1 - transition - night,
                    1f, nightLightLevel);
        }
        else if (t <= 1 - transition) {
            environmentLight = nightLightLevel;
        }
        else {
            environmentLight = Functions.clampedLinearRemap(
                    t, 1 - transition, 1, nightLightLevel, 1f);
        }
    }

    public float getCellLight(int x, int y) {
        return lightMap[x][y];
    }

    private float getLightLevel(float x, float y) {
        int i = (int) ((x - xMin) / cellSizeX);
        int j = (int) ((y - yMin) / cellSizeY);
        if (i < 0 || i >= width || j < 0 || j >= height)
            return 1f;
        return lightMap[i][j];
    }

    public float getLightLevel(Vector2 pos) {
        float light = getLightLevel(pos.x, pos.y);
        return MathUtils.clamp(environmentLight * light, 0f, 1f);
    }

    public void setCellLight(int x, int y, float light) {
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

    public float getEnvLight() {
        return environmentLight;
    }
}
