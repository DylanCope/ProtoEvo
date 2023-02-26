package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.MeatCell;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.physics.Particle;
import com.protoevo.physics.SpatialHash;
import com.protoevo.settings.SimulationSettings;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Chunks {

    private transient ConcurrentHashMap<Class<? extends Cell>, SpatialHash<Cell>> cellHashes;
    private ConcurrentHashMap<Class<? extends Cell>, Integer> globalCellCounts, globalCaps;

    public void initialise() {
        cellHashes = new ConcurrentHashMap<>(3, 1);
        globalCellCounts = new ConcurrentHashMap<>(3, 1);
        globalCaps = new ConcurrentHashMap<>(3, 1);

        globalCaps.put(Protozoan.class, SimulationSettings.maxProtozoa);
        globalCaps.put(PlantCell.class, SimulationSettings.maxPlants);
        globalCaps.put(MeatCell.class, SimulationSettings.maxMeat);

        int resolution = SimulationSettings.spatialHashResolution;
        int protozoaLocalCap = SimulationSettings.protozoaLocalCap;
        int plantLocalCap = SimulationSettings.plantLocalCap;
        int meatLocalCap = SimulationSettings.meatLocalCap;
        cellHashes.put(Protozoan.class, new SpatialHash<>(resolution, protozoaLocalCap, SimulationSettings.spatialHashRadius));
        cellHashes.put(PlantCell.class, new SpatialHash<>(resolution, plantLocalCap, SimulationSettings.spatialHashRadius));
        cellHashes.put(MeatCell.class, new SpatialHash<>(resolution, meatLocalCap, SimulationSettings.spatialHashRadius));
    }

    public void add(Cell cell) {
        cellHashes.get(cell.getClass()).add(cell, cell.getPos());
    }

    public int getLocalCount(Class<? extends Cell> cellClass) {
        return cellHashes.get(cellClass).size();
    }

    public int getGlobalCount(Cell cell) {
        Class<? extends Cell> cellClass = cell.getClass();
        if (!globalCellCounts.containsKey(cellClass)) {
            int count = getSpatialHash(cellClass).getChunkIndices().stream()
                    .mapToInt(i -> getSpatialHash(cellClass).getCount(i))
                    .sum();
            globalCellCounts.put(cellClass, count);
        }
        return globalCellCounts.get(cellClass);
    }

    public int getGlobalCapacity(Cell cell) {
        if (!globalCaps.containsKey(cell.getClass()))
            return 0;
        return globalCaps.get(cell.getClass());
    }

    public void clear() {
        cellHashes.values().forEach(SpatialHash::clear);
        globalCellCounts.clear();
    }

    public void allocate(Cell cell) {
        cellHashes.get(cell.getClass()).add(cell, cell.getPos());
    }

    public int getChunkCount(Class<? extends Particle> cellType, Vector2 pos) {
        return cellHashes.get(cellType).getCount(pos);
    }

    public int getChunkCapacity(Class<? extends Particle> cellType) {
        return cellHashes.get(cellType).getChunkCapacity();
    }

    public SpatialHash<Cell> getSpatialHash(Class<? extends Cell> clazz) {
        return cellHashes.get(clazz);
    }

    public Stream<Cell> getChunkStream(int i) {
        return cellHashes.values().stream()
                .flatMap(hash -> hash.getChunkContents(i).stream());
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
