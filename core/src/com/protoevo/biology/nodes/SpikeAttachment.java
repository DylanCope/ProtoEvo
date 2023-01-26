package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.Cell;
import com.protoevo.core.Simulation;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.utils.Utils;

public class SpikeAttachment extends NodeAttachment {

    private final Vector2 spikePoint = new Vector2();
    private Cell cell;
    private final float attackFactor = 10f;

    public SpikeAttachment(SurfaceNode node) {
        super(node);
    }

    public float getSpikeScalar() {
        // hard coded to according the 40 pixels of the spike sprite
        // and the 128 pixels of the cell sprite
        return 1f + 40f / 128f;
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        cell = getNode().getCell();
        spikePoint.set(node.getRelativePos()).scl(getSpikeScalar());
        spikePoint.add(cell.getPos());

        for (Object toInteract : cell.getInteractionQueue()) {
            if (toInteract instanceof Cell) {
                Cell other = (Cell) toInteract;
                if (other.isPointInside(spikePoint)) {
                    float r2 = other.getRadius() * other.getRadius();
                    float woundDepth = 1f - other.getPos().dst2(spikePoint) / r2;

                    float myAttack = (
                            2*cell.getHealth() +
                            ProtozoaSettings.spikeDamage * woundDepth +
                            2* Simulation.RANDOM.nextFloat()
                    );
                    float theirDefense = other.getShieldFactor() * (
                            2*other.getHealth() +
                            2*Simulation.RANDOM.nextFloat()
                    );

                    if (myAttack > theirDefense) {
                        float dps = attackFactor * (myAttack - theirDefense);
                        other.damage(dps * delta, CauseOfDeath.MURDER);
                    }
                }
            }
        }
    }

    @Override
    public float getInteractionRange() {
        return 1.1f * node.getCell().getRadius() * getSpikeScalar();
    }

    @Override
    public String getName() {
        return "Spike";
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
