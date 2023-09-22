package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.MeatCell;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.physics.SpatialHash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Chunks implements Serializable {
    public static final long serialVersionUID = 1L;

    private ConcurrentHashMap<Class<? extends Cell>, SpatialHash<Cell>> cellHashes;
    private ConcurrentHashMap<Class<? extends Cell>, Integer> globalCellCounts;

    public void initialise() {
        cellHashes = new ConcurrentHashMap<>(3, 1);
        globalCellCounts = new ConcurrentHashMap<>(3, 1);

        int resolution = Environment.settings.misc.spatialHashResolution.get();
        int protozoaLocalCap = Environment.settings.misc.protozoaLocalCap.get();
        int plantLocalCap = Environment.settings.misc.plantLocalCap.get();
        int meatLocalCap = Environment.settings.misc.meatLocalCap.get();
        float hashRadius = 1.5f * Environment.settings.worldgen.radius.get();
        cellHashes.put(Protozoan.class, new SpatialHash<>(resolution, protozoaLocalCap, hashRadius));
        cellHashes.put(PlantCell.class, new SpatialHash<>(resolution, plantLocalCap, hashRadius));
        cellHashes.put(MeatCell.class, new SpatialHash<>(resolution, meatLocalCap, hashRadius));
    }

    public void add(Cell cell) {
        cellHashes.get(cell.getClass()).add(cell, cell.getPos());
    }

    public int getLocalCount(Class<? extends Cell> cellClass) {
        return cellHashes.get(cellClass).size();
    }

    public int getGlobalCount(Cell cell) {
        return getGlobalCount(cell.getClass());
    }

    public int getGlobalCount(Class<? extends Cell> cellClass) {
        if (!globalCellCounts.containsKey(cellClass)) {
            int count = getSpatialHash(cellClass).getChunkIndices().stream()
                    .mapToInt(i -> getSpatialHash(cellClass).getCount(i))
                    .sum();
            globalCellCounts.put(cellClass, count);
        }
        return globalCellCounts.get(cellClass);
    }

    public int getGlobalCapacity(Cell cell) {
        return getGlobalCapacity(cell.getClass());
    }

    public int getGlobalCapacity(Class<? extends Cell> cellClass) {
        if (cellClass.equals(Protozoan.class))
            return Environment.settings.misc.maxProtozoa.get();
        else if (cellClass.equals(PlantCell.class))
            return Environment.settings.misc.maxPlants.get();
        else if (cellClass.equals(MeatCell.class))
            return Environment.settings.misc.maxMeat.get();
        return 0;
    }

    public void clear() {
        cellHashes.values().forEach(SpatialHash::clear);
        globalCellCounts.clear();
    }

    public void allocate(Cell cell) {
        cellHashes.get(cell.getClass()).add(cell, cell.getPos());
    }

    public SpatialHash<? extends Cell> getCellHash(Class<? extends Cell> cellType, Vector2 pos) {
        return cellHashes.get(cellType);
    }

    public int getChunkCount(Class<? extends Cell> cellType, Vector2 pos) {
        return cellHashes.get(cellType).getCount(pos);
    }

    public int getChunkCapacity(Class<? extends Cell> cellType) {
        return cellHashes.get(cellType).getChunkCapacity();
    }

    public SpatialHash<Cell> getSpatialHash(Class<? extends Cell> clazz) {
        return cellHashes.get(clazz);
    }

    public Stream<Cell> getChunkStream(int i) {
        return cellHashes.values().stream()
                .flatMap(hash -> hash.getChunkContents(i).stream());
    }

    public List<Cell> getChunkCells(int i) {
        List<Cell> chunkCells = new ArrayList<>();
        for (SpatialHash<Cell> hash : cellHashes.values()) {
            chunkCells.addAll(hash.getChunkContents(i));
        }
        return chunkCells;
    }

    public Collection<Integer> getChunkIndices() {
        return cellHashes.values()
                .stream()
                .flatMap(hash -> hash.getChunkIndices().stream())
                .collect(Collectors.toSet());
    }

    public Stream<Stream<Cell>> getStreams() {
        return getChunkIndices().stream().map(this::getChunkStream);
    }
}
