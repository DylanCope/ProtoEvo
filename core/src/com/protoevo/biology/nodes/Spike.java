package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Simulation;
import com.protoevo.settings.ProtozoaSettings;
import com.protoevo.utils.Geometry;
import com.protoevo.utils.Utils;

import java.io.Serial;
import java.io.Serializable;

public class Spike extends NodeAttachment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Vector2 spikePoint = new Vector2();
    private Cell cell;
    private final float attackFactor = 10f;
    private float extension = 1;

    public Spike(SurfaceNode node) {
        super(node);
    }

    public float getSpikeLength() {
        return node.getCell().getRadius() * getConstructionProgress() * .75f;
    }

    public float getSpikeExtension() {
        return extension;
    }

    public Vector2 getSpikePoint() {
        return spikePoint.set(node.getRelativePos())
                .setLength(getSpikeLength() * extension)
                .add(node.getWorldPosition());
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        cell = node.getCell();
        if (cell == null)
            return;

        extension = Utils.linearRemap(input[0], -1, 1, 0, 1);
        spikePoint = getSpikePoint();

        for (Object toInteract : cell.getInteractionQueue()) {
            if (toInteract instanceof Cell) {
                Cell other = (Cell) toInteract;
                Vector2 dir = spikePoint.cpy().sub(node.getWorldPosition());
                Vector2 start = node.getWorldPosition().cpy().sub(other.getPos());
                float[] ts = Geometry.circleIntersectLineTs(dir, start, other.getRadius());
                if (Geometry.lineIntersectCondition(ts)) {
                    output[0] = 1f;

//                    float r2 = other.getRadius() * other.getRadius();
//                    float woundDepth = 1f - other.getPos().dst2(spikePoint) / r2;
                    float woundDepth = MathUtils.clamp(
                            Math.max(ts[0], ts[1]) - Math.min(ts[0], ts[1]),
                            0, 1);

                    float myAttack = (
                            2*cell.getHealth() +
                            ProtozoaSettings.spikeDamage * woundDepth * getSpikeLength() / other.getRadius() +
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
