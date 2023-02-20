package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Simulation;
import com.protoevo.settings.ProtozoaSettings;
import com.protoevo.utils.Utils;

import java.io.Serial;
import java.io.Serializable;

public class Spike extends NodeAttachment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Vector2 spikePoint = new Vector2();
    private Cell cell;
    private final float attackFactor = 10f;
    private float extension = 1;

    public Spike(SurfaceNode node) {
        super(node);
    }

    public float getSpikeScalar() {
        // TODO: update this to use the actual spike sprite size
        // hard coded to according the 40 pixels of the spike sprite
        // and the 128 pixels of the cell sprite
        return extension * getConstructionProgress() * (1f + 70f / 128f);
    }

    public float getSpikeLength() {
        return node.getCell().getRadius() * getConstructionProgress() / 4f;
    }

    public float getSpikeExtension() {
        return extension;
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        cell = getNode().getCell();
        extension = Utils.linearRemap(input[0], -1, 1, 0, 1);
        spikePoint.set(node.getRelativePos())
                .setLength(getSpikeLength() * extension)
                .add(cell.getPos());

        for (Object toInteract : cell.getInteractionQueue()) {
            if (toInteract instanceof Cell) {
                Cell other = (Cell) toInteract;
                if (other.isPointInside(spikePoint)) {
                    output[0] = 1f;

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

                    output[1] = other.getHealth();
                    output[2] = myAttack - theirDefense;
                }
            }
        }
    }

    @Override
    public float getInteractionRange() {
        return 1.05f * getSpikeLength() * extension + node.getCell().getRadius();
    }

    @Override
    public String getName() {
        return "Spike";
    }

    @Override
    public String getInputMeaning(int index) {
        if (index == 0)
            return "Extension";
        return null;
    }

    @Override
    public String getOutputMeaning(int index) {
        if (index == 0)
            return "Did Hit?";
        if (index == 1)
            return "Attacked Health";
        if (index == 2)
            return "Attack Amount";
        return null;
    }
}
