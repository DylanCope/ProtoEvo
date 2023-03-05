package com.protoevo.env;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.utils.Geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldGeneration {
    
    public static Random RANDOM = new Random(Environment.settings.world.seed.get());

    public static List<Rock> generate() {
        RANDOM = new Random(Environment.settings.world.seed.get());
        
        List<Rock> rocks = new ArrayList<>();

        float minR = Environment.settings.world.rockClusterRadius.get();
        float maxR = 4 * Environment.settings.world.rockClusterRadius.get();
        int numClusterCentres = Environment.settings.world.numRingClusters.get();

        WorldGeneration.generateClustersOfRocks(
                rocks, numClusterCentres, minR, maxR,
                Environment.settings.world.radius.get() - maxR);

        generateRocks(rocks, Environment.settings.world.rockGenerationIterations.get());
        
        return rocks;
    }

    public static Vector2 randomPosition(Vector2 centre, float minR, float maxR) {
        float t = (float) (2 * Math.PI * RANDOM.nextDouble());
        float r = minR + (maxR - minR) * RANDOM.nextFloat();
        return Geometry.fromAngle(t).scl(r).add(centre);
    }

    public static Vector2 randomPosition(float entityRadius) {
        return randomPosition(Geometry.ZERO, entityRadius, Environment.settings.world.radius.get());
    }

    public static Vector2 randomPosition(float minR, float maxR) {
        return randomPosition(Geometry.ZERO, minR, maxR);
    }

    public static void generateClustersOfRocks(
            List<Rock> rocks, int nRings, float minR, float maxR, float centreOffset) {
        for (int i = 0; i < nRings; i++) {
            Vector2 centre = Geometry.polarRandomPointInCircle(centreOffset, RANDOM);
            float radius = RANDOM.nextFloat() * (maxR - minR) + minR;
            generateRingOfRocks(rocks, centre, radius);
        }
    }

    public static void generateRingOfRocks(List<Rock> rocks, Vector2 ringCentre, float ringRadius) {
        generateRingOfRocks(rocks, ringCentre, ringRadius, Environment.settings.world.ringBreakProbability.get());
    }

    public static void generateRingOfRocks(List<Rock> rocks, Vector2 ringCentre, float ringRadius, float breakProb) {
        float angleDelta = (float) (2 * Math.asin(Environment.settings.world.minRockSize.get() / (20 * ringRadius)));
        Rock currentRock = null;
        for (float angle = 0; angle < 2*Math.PI; angle += angleDelta) {
            if (breakProb > 0 && RANDOM.nextFloat() < breakProb) {
                currentRock = null;
                angle += Environment.settings.world.ringBreakAngleMinSkip.get() +
                        RANDOM.nextFloat() * (Environment.settings.world.ringBreakAngleMaxSkip.get()
                                                - Environment.settings.world.ringBreakAngleMinSkip.get());
            }
            if (currentRock == null || currentRock.allEdgesAttached()) {
                currentRock = newCircumferenceRockAtAngle(ringCentre, ringRadius, angle);
                if (isRockObstructed(currentRock, rocks, Environment.settings.world.minRockOpeningSize.get())) {
                    currentRock = null;
                } else {
                    rocks.add(currentRock);
                }
            } else {
                Rock bestNextRock = null;
                float bestRockDistToCirc = Float.MAX_VALUE;
                int bestRockAttachIdx = -1;
                for (int i = 0; i < currentRock.getEdges().length; i++) {
                    float sizeRange = (Environment.settings.world.maxRockSize.get()
                            - Environment.settings.world.minRockOpeningSize.get());
                    float rockSize = 1.5f * Environment.settings.world.minRockOpeningSize.get()
                            + sizeRange * RANDOM.nextFloat();
                    if (!currentRock.isEdgeAttached(i)) {
                        Rock newRock = newAttachedRock(currentRock, i, rocks, rockSize);
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
                    rocks.add(bestNextRock);
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


    public static void generateRocks(List<Rock> rocks, int nIterations) {
        List<Rock> unattachedRocks = new ArrayList<>();
        for (Rock rock : rocks)
            if (!rock.allEdgesAttached())
                unattachedRocks.add(rock);

        for (int i = 0; i < nIterations; i++) {
            if (unattachedRocks.size() == 0
                    || RANDOM.nextFloat() > Environment.settings.world.rockClustering.get()) {
                Rock rock = newRock(rocks);
                if (tryAdd(rock, rocks)) {
                    unattachedRocks.add(rock);
                }
            } else {
                Rock toAttach = selectRandomUnattachedRock(unattachedRocks);
                for (int j = 0; j < 50; j++) {
                    if (toAttach == null)
                        break;

                    boolean added = false;
                    for (int edgeIdx = 0; edgeIdx < 3; edgeIdx++) {
                        if (!toAttach.isEdgeAttached(edgeIdx)) {
                            Rock rock = newAttachedRock(toAttach, edgeIdx, rocks);
                            if (rock != null) {
                                rocks.add(rock);
                                unattachedRocks.add(rock);
                                rock.setEdgeAttached(0);
                                toAttach.setEdgeAttached(edgeIdx);
                                if (edgeIdx == 2) // no edges left to attach to
                                    unattachedRocks.remove(toAttach);
                                toAttach = rock;
                                added = true;
                                break;
                            }
                        }
                    }

                    if (!added) {
                        break;
                    }
                }
            }
        }
    }

    public static Rock newAttachedRock(Rock toAttach, int edgeIdx, List<Rock> rocks) {
        float attachedSize = toAttach.getSize();
        float sizeMin = Math.max(
                Environment.settings.world.minRockSize.get(),
                attachedSize * (1 - Environment.settings.world.attachedRockSizeChange.get()));
        float sizeMax = Math.min(
                Environment.settings.world.maxRockSize.get(),
                attachedSize * (1 + Environment.settings.world.attachedRockSizeChange.get()));
        float sizeRange = (sizeMax - sizeMin);
        float rockSize = sizeMin + sizeRange * RANDOM.nextFloat();
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
                && leavesOpening(p3, rocks, Environment.settings.world.minRockOpeningSize.get())) {
            return new Rock(p1, p2, p3);
        }
        return null;
    }

    private static Rock selectRandomUnattachedRock(List<Rock> unattachedRocks) {
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

    public static Rock newRock(List<Rock> rocks) {
        float centreR = Environment.settings.world.radius.get() * RANDOM.nextFloat();
        float centreT = (float) (2*Math.PI * RANDOM.nextFloat());
        Vector2 centre = Geometry.fromAngle(centreT).setLength(centreR);
        return newRockAt(centre);
    }

    public static Rock newRockAt(Vector2 centre) {
        Vector2 dir = Geometry.fromAngle((float) (2 * Math.PI * RANDOM.nextFloat()));
        return newRockAt(centre, dir);
    }

    public static Rock newRockAt(Vector2 centre, Vector2 dir) {
        float sizeRange = (Environment.settings.world.maxRockSize.get()
                - Environment.settings.world.minRockSize.get());
        float rockSize = Environment.settings.world.minRockSize.get()
                + sizeRange * RANDOM.nextFloat();

        Vector2 p1 = centre.cpy().add(dir.cpy().setLength(rockSize));

        float dt = Environment.settings.world.minRockSpikiness.get() / 2f;
        float tMin = 2 * MathUtils.PI / 3 - dt;
        float tMax = 2 * MathUtils.PI / 3 + dt;

        float t1 = tMin + (tMax - tMin) * RANDOM.nextFloat();
        Vector2 p2 = centre.cpy().add(dir.cpy().rotateRad(t1).setLength(rockSize));

        float t2 = tMin + (tMax - tMin) * RANDOM.nextFloat();
        Vector2 p3 = centre.cpy().add(dir.cpy().rotateRad(t1 + t2).setLength(rockSize));

        return new Rock(p1, p2, p3);

    }
}
