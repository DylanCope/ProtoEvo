package com.protoevo.biology.cells;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.env.Environment;
import com.protoevo.env.Spawnable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates a representation of a multicellular structure.
 */
public class MultiCellStructure implements Serializable, Spawnable {
    public static final long serialVersionUID = 1L;

    private final Map<Integer, List<Long>> cellConnections = new HashMap<>();
    private final Map<Integer, Vector2> relativeCellPositions = new HashMap<>();
    private List<Cell> cells;

    public MultiCellStructure() {}

    public MultiCellStructure(Set<Long> cellIds, Environment environment) {
        cells = cellIds.stream()
                .map(cellID -> environment.getCell(cellID)
                        .orElseThrow(() -> new RuntimeException("Cell not found")))
                .collect(Collectors.toList());

        Vector2 centre = computeMultiCellCentre();

        int localCellID = 0;
        for (Cell cell : cells) {
            List<Long> attachedCellIds = new ArrayList<>(cell.getAttachedCellIDs());
            cellConnections.put(localCellID, attachedCellIds);
            Vector2 relativePos = cell.getPos().cpy().sub(centre);
            relativeCellPositions.put(localCellID, relativePos);
            localCellID++;
        }
    }

    public boolean stillConnected() {
        for (int localCellID = 0; localCellID < cells.size(); localCellID++) {
            Cell cell = cells.get(localCellID);
            List<Long> expectedAttachedCellIds = cellConnections.get(localCellID);
            Collection<Long> actualAttachedCellIds = cell.getAttachedCellIDs();
            for (Long expectedAttachedCellId : expectedAttachedCellIds) {
                if (!actualAttachedCellIds.contains(expectedAttachedCellId)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Vector2 computeMultiCellCentre() {
        Vector2 centre = new Vector2(0, 0);
        for (Cell cell : cells) {
            centre.add(cell.getPos());
        }
        centre.scl(1.0f / cells.size());
        return centre;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public float computeMultiCellRadius() {
        Vector2 centre = computeMultiCellCentre();
        float radius = 0f;
        for (Cell cell : cells) {
            float dist = cell.getPos().dst(centre) + cell.getRadius();
            if (dist > radius) {
                radius = dist;
            }
        }
        return radius;
    }

    @Override
    public void spawn(Environment environment, float x, float y) {
        for (int localCellID = 0; localCellID < cells.size(); localCellID++) {
            Cell cell = cells.get(localCellID);
            Vector2 relativePos = relativeCellPositions.get(localCellID);
            float thisX = x + relativePos.x;
            float thisY = y + relativePos.y;
            cell.spawn(environment, thisX, thisY);
        }
    }
}
