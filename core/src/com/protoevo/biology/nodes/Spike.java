package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.utils.Geometry;
import com.protoevo.utils.Utils;


import java.io.Serializable;

public class Spike extends NodeAttachment implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private Vector2 spikePoint = new Vector2();
    private final float attackFactor = 10f;
    private float lastDPS = 0;
    private float extension = 1;
    private float myLastAttack = 0, theirLastDefense = 0;

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
        Cell cell = node.getCell();
        if (cell == null)
            return;

        extension = Utils.clampedLinearRemap(input[0], -1, 1, 0, 1);
        spikePoint = getSpikePoint();

        for (Object toInteract : cell.getInteractionQueue()) {
            if (toInteract instanceof Cell) {
                Cell other = (Cell) toInteract;
                Vector2 dir = spikePoint.cpy().sub(node.getWorldPosition());
                Vector2 start = node.getWorldPosition().cpy().sub(other.getPos());
                float[] ts = Geometry.circleIntersectLineTs(dir, start, other.getRadius());
                if (Geometry.lineIntersectCondition(ts)) {
                    output[0] = 1f;

                    float woundDepth = MathUtils.clamp(
                            Math.max(ts[0], ts[1]) - Math.min(ts[0], ts[1]),
                            0, 1);

                    myLastAttack = (
                            2* cell.getHealth() +
                            Environment.settings.protozoa.spikeDamage.get() *
                                    woundDepth * getSpikeLength() / other.getRadius() +
                            2* MathUtils.random()
                    );
                    theirLastDefense = other.getShieldFactor() * (
                            2*other.getHealth() +
                            2*MathUtils.random()
                    );

                    if (myLastAttack > theirLastDefense) {
                        float dps = attackFactor * (myLastAttack - theirLastDefense);
                        other.damage(dps * delta, CauseOfDeath.SPIKE_DAMAGE);
                        lastDPS = dps;
                    }
                    else {
                        lastDPS = 0;
                    }

                    if (output.length > 2) {
                        output[1] = other.getHealth();
                        output[2] = myLastAttack - theirLastDefense;
                    }
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
        if (node.getIODimension() == 3) {
            if (index == 1)
                return "Attacked Health";
            if (index == 2)
                return "Attack Amount";
        }
        return null;
    }

    @Override
    public void addStats(Statistics stats) {
        stats.put("Last DPS", lastDPS, Statistics.ComplexUnit.PERCENTAGE_PER_TIME);
        stats.put("Spike Length", getSpikeLength(), Statistics.ComplexUnit.DISTANCE);
        stats.put("Spike Extension", getSpikeExtension(), Statistics.ComplexUnit.PERCENTAGE);
        stats.put("My Last Attack", myLastAttack);
        stats.put("Their Last Defense", theirLastDefense);
    }
}
