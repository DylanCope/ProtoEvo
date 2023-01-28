package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.Cell;
import com.protoevo.biology.EdibleCell;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.env.CollisionHandler;

public class PhagocyticReceptor extends NodeAttachment {
    private final Vector2 tmp = new Vector2();

    public PhagocyticReceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        Cell cell = node.getCell();
        if (!(cell instanceof Protozoan)) {
            return;
        }

        for (CollisionHandler.FixtureCollision contact : cell.getContacts()) {
            Object collided = cell.getOther(contact);
            if (collided instanceof Cell && engulfCondition((Cell) collided)) {
                engulf((Cell) collided);
            }
        }
    }

    private boolean engulfCondition(Cell other) {
        return other instanceof EdibleCell
                && correctSizes(other) && notEngulfed(other)
                && closeEnough(other) && roomFor(other);
    }

    private boolean roomFor(Cell other) {
        float areaAvailable = .8f * node.getCell().getArea();
        for (Cell c : ((Protozoan) node.getCell()).getEngulfedCells()) {
            areaAvailable -= c.getArea();
        }
        return areaAvailable > other.getArea();
    }

    private boolean correctSizes(Cell other) {
        Cell cell = node.getCell();
        return other.getRadius() < cell.getRadius() / 2
                && cell.getRadius() > 2 * SimulationSettings.minParticleRadius;
    }

    private boolean notEngulfed(Cell other) {
        return !((Protozoan) node.getCell()).getEngulfedCells().contains(other);
    }

    private boolean closeEnough(Cell other) {
        Vector2 nodePos = node.getWorldPosition();
        float d = other.getRadius() + node.getCell().getRadius() / 3f;
        return nodePos.dst2(other.getPos()) < d*d;
    }

    public void engulf(Cell cell) {
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
        return null;
    }

    @Override
    public String getOutputMeaning(int index) {
        return null;
    }
}
