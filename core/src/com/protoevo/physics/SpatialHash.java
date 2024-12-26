package com.protoevo.physics;

import com.badlogic.gdx.math.Vector2;


import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class SpatialHash<T> implements Serializable, Iterable<Collection<T>> {
    
    private static final long serialVersionUID = 1L;

    private int resolution;
    private float chunkSize, worldRadius;
    private int maxObjectsPerChunk;
    private ConcurrentHashMap<Integer, Collection<T>> chunkContents;
    private int size = 0;

    public SpatialHash() {}

    public SpatialHash(int resolution, int maxObjectsPerChunk, float worldRadius) {
        this.resolution = resolution;
        this.maxObjectsPerChunk = maxObjectsPerChunk;
        this.chunkSize = 2 * worldRadius / resolution;
        this.worldRadius = worldRadius;
        this.chunkContents = new ConcurrentHashMap<>();
    }

    public SpatialHash(int resolution, float worldRadius) {
        this(resolution, Integer.MAX_VALUE, worldRadius);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getCount(Vector2 worldPos) {
        int i = getChunkX(worldPos.x);
        int j = getChunkY(worldPos.y);
        return getCount(i, j);
    }

    public int getCount(int i, int j) {
        int idx = getChunkIndex(i, j);
        return getCount(idx);
    }

    public int getCount(int idx) {
        if (chunkContents.containsKey(idx))
            return chunkContents.get(idx).size();
        return 0;
    }

    public boolean isFull(Vector2 worldPos) {
        return getCount(worldPos) >= maxObjectsPerChunk - 1;
    }

    public int getChunkX(float x) {
        return (int) (x / chunkSize);
    }

    public int getChunkY(float y) {
        return (int) (y / chunkSize);
    }

    private int getChunkIndex(int i, int j) {
        return i * resolution + j;
    }

    public boolean add(T t, int i, int j) {
        int idx = getChunkIndex(i, j);

        Collection<T> chunk;
        if (!chunkContents.containsKey(idx)) {
            chunk = new ConcurrentSkipListSet<>(Comparator.comparingInt(Object::hashCode));
            chunkContents.put(idx, chunk);
        } else {
            chunk = chunkContents.get(idx);
        }

        if (chunk.size() >= maxObjectsPerChunk)
            return false;

        chunk.add(t);
        size++;

        return true;
    }

    public boolean add(T t, Vector2 pos) {

        int x = getChunkX(pos.x);
        int y = getChunkY(pos.y);

        return add(t, x, y);
    }

    public boolean add(T t, Vector2[] bounds) {
        int x1 = getChunkX(bounds[0].x);
        int y1 = getChunkY(bounds[0].y);
        int x2 = getChunkX(bounds[1].x);
        int y2 = getChunkY(bounds[1].y);

        if (x1 > x2) {
            int tmp = x1;
            x1 = x2;
            x2 = tmp;
        }
        if (y1 > y2) {
            int tmp = y1;
            y1 = y2;
            y2 = tmp;
        }

        boolean added = false;
        for (int i = x1; i <= x2; i++) {
            for (int j = y1; j <= y2; j++) {
                added |= add(t, i, j);
            }
        }

        return added;
    }

    public void clear() {
        size = 0;
        chunkContents.values().forEach(Collection::clear);
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

    public float getChunkSize() {
        return chunkSize;
    }

    @Override
    public Iterator<Collection<T>> iterator() {
        return chunkContents.values().iterator();
    }

    public Collection<Integer> getChunkIndices() {
        return chunkContents.keySet();
    }

    public Collection<T> getChunkContents(int i) {
        if (chunkContents.containsKey(i))
            return chunkContents.get(i);
        return Collections.emptyList();
    }

    public Collection<T> getChunkContents(int i, int j) {
        int idx = getChunkIndex(i, j);
        return getChunkContents(idx);
    }
}
