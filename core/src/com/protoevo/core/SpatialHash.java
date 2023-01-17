package com.protoevo.core;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class SpatialHash<T extends Shape> {

    private final int resolution;
    private final float cellSize, worldRadius;
    private final int maxObjectsPerChunk;
//    private final Shape[][][] objects;
    private final ConcurrentHashMap<Integer, Integer> chunkCounts;
    private int size = 0;

    public SpatialHash(int resolution, int maxObjectsPerChunk, float worldRadius) {
        this.resolution = resolution;
        this.maxObjectsPerChunk = maxObjectsPerChunk;
        this.cellSize = 2 * worldRadius / resolution;
        this.worldRadius = worldRadius;
//        this.objects = new Shape[resolution][resolution][maxObjectsPerChunk];
        this.chunkCounts = new ConcurrentHashMap<>();
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getCount(int i, int j) {
        return chunkCounts.getOrDefault(getChunkIndex(i, j), 0);
    }

    public int getCount(Vector2 worldPos) {
        int i = getChunkX(worldPos.x);
        int j = getChunkY(worldPos.y);
        return getCount(i, j);
    }

    public boolean isFull(Vector2 worldPos) {
        return getCount(worldPos) >= maxObjectsPerChunk - 1;
    }

    public int getChunkX(float x) {
        return MathUtils.clamp(
                (int) ((x + worldRadius) / cellSize),
                0, resolution - 1);
    }

    public int getChunkY(float y) {
        return MathUtils.clamp(
                (int) ((y + worldRadius) / cellSize),
                0, resolution - 1);
    }

    private int getChunkIndex(int i, int j) {
        return i * resolution + j;
    }

    public boolean add(T t, int i, int j) {
        int count = getCount(i, j);
        chunkCounts.put(getChunkIndex(i, j), count + 1);
        size++;
        return false;
    }

    public boolean add(T t) {
        Vector2 pos = t.getPos();

        int minX = getChunkX(pos.x);
        int minY = getChunkY(pos.y);

        return add(t, minX, minY);
    }

    public boolean addAll(Collection<? extends T> c) {
        boolean added = true;
        for (T t : c)
            added &= add(t);
        return added;
    }

    public void clear() {
        size = 0;
        chunkCounts.clear();
//        Arrays.fill(objects, null);
    }

    public int getChunkCapacity() {
        return maxObjectsPerChunk;
    }

    public int getTotalCapacity() {
        return maxObjectsPerChunk * resolution * resolution;
    }

    public int getResolution() {
        return resolution;
    }

    public float getOriginX() {
        return -worldRadius;
    }

    public float getOriginY() {
        return -worldRadius;
    }

    public float getCellSize() {
        return cellSize;
    }
}
