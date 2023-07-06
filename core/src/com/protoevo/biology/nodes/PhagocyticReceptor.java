package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.cells.*;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.physics.Collision;
import com.protoevo.physics.Particle;


import java.io.Serializable;

public class PhagocyticReceptor extends NodeAttachment implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private final Vector2 tmp = new Vector2();
    private Cell lastEngulfed;
    private boolean engulfPlant, engulfMeat;

    public PhagocyticReceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        Cell cell = node.getCell();
        if (!(cell instanceof Protozoan)) {
            return;
        }

        engulfPlant = input[0] > 0f;
        engulfMeat = input[1] > 0f;

        if (!((Protozoan) cell).getEngulfedCells().contains(lastEngulfed))
            lastEngulfed = null;
        if (lastEngulfed != null) {
            output[0] = lastEngulfed.getHealth();
            if (lastEngulfed instanceof PlantCell)
                output[1] = 1f;
            else if (lastEngulfed instanceof MeatCell)
                output[2] = 1f;
        }

        for (Collision contact : cell.getParticle().getContacts()) {
            Object collided = contact.getOther(cell.getParticle());
            if (!(collided instanceof Particle
                    && ((Particle) collided).getUserData() instanceof Cell))
                continue;

            Cell collidingCell = ((Particle) collided).getUserData(Cell.class);
            if (engulfCondition(collidingCell)) {
                engulf(collidingCell);
            }
        }
    }

    private boolean engulfCondition(Cell other) {
        if (other instanceof PlantCell && !engulfPlant)
            return false;
        if (other instanceof MeatCell && !engulfMeat)
            return false;
        return other.isEdible()
                && correctSizes(other) && notEngulfed(other)
                && closeEnough(other) && roomFor(other);
    }

    private boolean roomFor(Cell other) {
        float areaAvailable = .8f * node.getCell().getParticle().getArea();
        for (Cell c : ((Protozoan) node.getCell()).getEngulfedCells()) {
            areaAvailable -= c.getParticle().getArea();
        }
        return areaAvailable > other.getParticle().getArea();
    }

    private boolean correctSizes(Cell other) {
        Cell cell = node.getCell();
        float progressFactor = 0.5f + 0.5f * getConstructionProgress();
        return other.getRadius() < progressFactor * cell.getRadius() * 0.8f
                && cell.getRadius() > 2 * Environment.settings.minParticleRadius.get();
    }

    private boolean notEngulfed(Cell other) {
        return !((Protozoan) node.getCell()).getEngulfedCells().contains(other);
    }

    private boolean closeEnough(Cell other) {
        Vector2 nodePos = node.getWorldPosition();
        float d = other.getRadius() + engulfRange();
        return nodePos.dst2(other.getPos()) < d*d;
    }

    public float engulfRange() {
        return node.getCell().getRadius() / 2f; // * Environment.settings.engulfRangeFactor.get();
    }

    public void engulf(Cell cell) {
        lastEngulfed = cell;
        cell.setEngulfer(node.getCell());
        cell.kill(CauseOfDeath.EATEN);
        ((Protozoan) node.getCell()).getEngulfedCells().add(cell);
    }

    @Override
    public String getName() {
        return "Phagocytic Receptor";
    }

    @Override
    public String getInputMeaning(int index) {
        if (index == 0)
            return "Should Engulf Plant";
        if (index == 1)
            return "Should Engulf Meat";
        return null;
    }

    @Override
    public String getOutputMeaning(int index) {
        if (index == 0)
            return "Last Engulfed Health";
        if (index == 1)
            return "Is Last Engulfed Plant";
        if (index == 2)
            return "Is Last Engulfed Meat";
        return null;
    }

    @Override
    public void addStats(Statistics stats) {
        stats.putCount("Engulfed Cells", ((Protozoan) node.getCell()).getEngulfedCells().size());
        stats.putBoolean("Will Engulf Plant?", engulfPlant);
        stats.putBoolean("Will Engulf Meat?", engulfMeat);
    }
}
