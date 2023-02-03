package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.settings.WorldGenerationSettings;
import com.protoevo.utils.Geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldGeneration {
    
    public static Random RANDOM = new Random(WorldGenerationSettings.worldGenerationSeed);

    public static Vector2 randomPosition(Vector2 centre, float minR, float maxR) {
        float t = (float) (2 * Math.PI * RANDOM.nextDouble());
        float r = minR + (maxR - minR) * RANDOM.nextFloat();
        return Geometry.fromAngle(t).scl(r).add(centre);
    }

    public static Vector2 randomPosition(float entityRadius) {
        return randomPosition(Geometry.ZERO, entityRadius, WorldGenerationSettings.environmentRadius);
    }

    public static Vector2 randomPosition(float minR, float maxR) {
        return randomPosition(Geometry.ZERO, minR, maxR);
    }

    public static void generateClustersOfRocks(Environment environment, Vector2 pos, int nRings, float radiusRange) {

        if (WorldGenerationSettings.initialPopulationClustering) {
            // generate a ring for each population cluster
            for (int i = 0; i < nRings; i++) {
                Vector2 centre = randomPosition(WorldGenerationSettings.rockClusterRadius).add(pos);
                float minR = Math.max(0.1f, radiusRange);
                float radius = RANDOM.nextFloat() * (radiusRange - minR) + minR;
                generateRingOfRocks(environment, centre, radius);
            }
        }
    }

    public static void generateRingOfRocks(Environment environment, Vector2 ringCentre, float ringRadius) {
        generateRingOfRocks(environment, ringCentre, ringRadius, WorldGenerationSettings.ringBreakProbability);
    }

    public static void generateRingOfRocks(Environment environment, Vector2 ringCentre, float ringRadius, float breakProb) {
        float angleDelta = (float) (2 * Math.asin(WorldGenerationSettings.minRockSize / (20 * ringRadius)));
        Rock currentRock = null;
        for (float angle = 0; angle < 2*Math.PI; angle += angleDelta) {
            if (breakProb > 0 && RANDOM.nextFloat() < breakProb) {
                currentRock = null;
                angle += RANDOM.nextFloat(
                        WorldGenerationSettings.ringBreakAngleMinSkip,
                        WorldGenerationSettings.ringBreakAngleMaxSkip);
            }
            if (currentRock == null || currentRock.allEdgesAttached()) {
                currentRock = newCircumferenceRockAtAngle(ringCentre, ringRadius, angle);
                if (isRockObstructed(currentRock, environment.getRocks(), WorldGenerationSettings.minRockOpeningSize)) {
                    currentRock = null;
                } else {
                    environment.getRocks().add(currentRock);
                }
            } else {
                Rock bestNextRock = null;
                float bestRockDistToCirc = Float.MAX_VALUE;
                int bestRockAttachIdx = -1;
                for (int i = 0; i < currentRock.getEdges().length; i++) {
                    float sizeRange = (WorldGenerationSettings.maxRockSize - WorldGenerationSettings.minRockOpeningSize);
                    float rockSize = 1.5f * WorldGenerationSettings.minRockOpeningSize + sizeRange * RANDOM.nextFloat();
                    if (!currentRock.isEdgeAttached(i)) {
                        Rock newRock = newAttachedRock(currentRock, i, environment.getRocks(), rockSize);
                        if (newRock != null) {
                            float dist = Math.abs(newRock.getCentre().dst(ringCentre) - ringRadius);
                            if (dist < bestRockDistToCirc) {
                                bestRockDistToCirc = dist;
                                bestNextRock = newRock;
                                bestRockAttachIdx = i;
                            }
                        }
                    }
                }
                if (bestNextRock != null) {
                    environment.getRocks().add(bestNextRock);
                    bestNextRock.setEdgeAttached(0);
                    currentRock.setEdgeAttached(bestRockAttachIdx);
                }
                currentRock = bestNextRock;
            }
        }
    }

    private static Rock newCircumferenceRockAtAngle(Vector2 pos, float r, float angle) {
        Vector2 dir = Geometry.fromAngle(angle);
        Vector2 centre = dir.cpy().setLength(r).add(pos);
        return newRockAt(centre, dir);
    }


    public static void generateRocks(Environment environment, int nIterations) {
        List<Rock> unattachedRocks = new ArrayList<>();
        for (Rock rock : environment.getRocks())
            if (!rock.allEdgesAttached())
                unattachedRocks.add(rock);

        for (int i = 0; i < nIterations; i++) {
            if (unattachedRocks.size() == 0
                    || RANDOM.nextFloat() > WorldGenerationSettings.rockClustering) {
                Rock rock = newRock(environment);
                if (tryAdd(rock, environment.getRocks())) {
                    unattachedRocks.add(rock);
                }
            } else {
                Rock toAttach = selectRandomUnattachedRock(environment, unattachedRocks);
                int edgeIdx = 0;
                while (edgeIdx < 3) {
                    if (!toAttach.isEdgeAttached(edgeIdx))
                        break;
                    edgeIdx++;
                }
                if (edgeIdx == 3)
                    continue;

                Rock rock = newAttachedRock(toAttach, edgeIdx, environment.getRocks());
                if (rock != null) {
                    environment.getRocks().add(rock);
                    unattachedRocks.add(rock);
                    rock.setEdgeAttached(0);
                    toAttach.setEdgeAttached(edgeIdx);
                    if (edgeIdx == 2) // no edges left to attach to
                        unattachedRocks.remove(toAttach);
                }
            }
        }
    }

    public static Rock newAttachedRock(Rock toAttach, int edgeIdx, List<Rock> rocks) {
        float sizeRange = (WorldGenerationSettings.maxRockSize - WorldGenerationSettings.minRockSize);
        float rockSize = WorldGenerationSettings.minRockSize + sizeRange * RANDOM.nextFloat();
        return newAttachedRock(toAttach, edgeIdx, rocks, rockSize);
    }

    public static Rock newAttachedRock(Rock toAttach, int edgeIdx, List<Rock> rocks, float rockSize) {
        Vector2[] edge = toAttach.getEdge(edgeIdx);
        Vector2 normal = toAttach.getNormals()[edgeIdx];
        Vector2 p1 = edge[0].cpy(), p2 = edge[1].cpy();

        Vector2 p3 = p1.cpy().add(p2).scl(0.5f).add(normal.cpy().setLength(rockSize));

        Vector2[] newEdge1 = new Vector2[]{p1, p3};
        Vector2[] newEdge2 = new Vector2[]{p2, p3};
        if (notInAnyRocks(newEdge1, newEdge2, rocks, toAttach)
                && leavesOpening(p3, rocks, WorldGenerationSettings.minRockOpeningSize)) {
            return new Rock(p1, p2, p3);
        }
        return null;
    }

    private static Rock selectRandomUnattachedRock(Environment environment, List<Rock> unattachedRocks) {
        int i = RANDOM.nextInt(unattachedRocks.size());
        return unattachedRocks.get(i);
    }

    private static boolean tryAdd(Rock rock, List<Rock> rocks) {
        for (Rock otherRock : rocks)
            if (rock.intersectsWith(otherRock))
                return false;
        rocks.add(rock);
        return true;
    }

    private static boolean isRockObstructed(Rock rock, List<Rock> rocks, float openingSize) {
        for (Rock otherRock : rocks)
            if (otherRock.intersectsWith(rock))
                return true;
        if (openingSize > 0)
            for (Vector2 point : rock.getPoints())
                if (!leavesOpening(point, rocks, openingSize))
                    return true;
        return false;
    }

    private static boolean notInAnyRocks(Vector2[] e1, Vector2[] e2, List<Rock> rocks, Rock excluding) {
        for (Rock rock : rocks)
            for (Vector2[] rockEdge : rock.getEdges())
                if (!rock.equals(excluding) &&
                        (Rock.edgesIntersect(rockEdge, e1) || Rock.edgesIntersect(rockEdge, e2)))
                    return false;
        return true;
    }

    private static boolean leavesOpening(Vector2 rockPoint, List<Rock> rocks, float openingSize) {
        for (Rock rock : rocks) {
            for (Vector2[] edge : rock.getEdges()) {
                if (Geometry.doesLineIntersectCircle(edge, rockPoint, openingSize))
                    return false;
            }
        }
        return true;
    }

    public static Rock newRock(Environment environment) {
        float centreR = WorldGenerationSettings.environmentRadius * RANDOM.nextFloat();
        float centreT = (float) (2*Math.PI * RANDOM.nextFloat());
        Vector2 centre = Geometry.fromAngle(centreT).setLength(centreR);
        return newRockAt(centre);
    }

    public static Rock newRockAt(Vector2 centre) {
        Vector2 dir = Geometry.fromAngle((float) (2 * Math.PI * RANDOM.nextFloat()));
        return newRockAt(centre, dir);
    }

    public static Rock newRockAt(Vector2 centre, Vector2 dir) {
        float sizeRange = (WorldGenerationSettings.maxRockSize - WorldGenerationSettings.minRockSize);
        float rockSize = WorldGenerationSettings.minRockSize + sizeRange * RANDOM.nextFloat();

        float k1 = 0.95f + 0.1f * RANDOM.nextFloat();
        Vector2 p1 = centre.cpy().add(dir.cpy().setLength(k1 * rockSize));
//(417.3416,-608.31537)
        float tMin = WorldGenerationSettings.minRockSpikiness;
        float tMax = (float) (2*Math.PI / 3);
        float t1 = tMin + (tMax - 2*tMin) * RANDOM.nextFloat();
        float k2 = 0.95f + 0.1f * RANDOM.nextFloat();

        dir.rotateRad(t1);
        Vector2 p2 = centre.cpy().add(dir.cpy().setLength(k2 * rockSize));
//(702.7173,224.46463)
        float t2 = tMin + (tMax - tMin) * RANDOM.nextFloat();
        float l3 = WorldGenerationSettings.minRockSize + sizeRange * RANDOM.nextFloat();
        dir.rotateRad(t2);
        Vector2 p3 = centre.cpy().add(dir.cpy().setLength(l3));

        return new Rock(p1, p2, p3);

    }
}
