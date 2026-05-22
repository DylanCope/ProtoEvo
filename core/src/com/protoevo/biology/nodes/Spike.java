package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.maths.Functions;
import com.protoevo.physics.Particle;
import com.protoevo.maths.Geometry;


import java.io.Serializable;

public class Spike extends NodeAttachment implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private Vector2 spikePoint = new Vector2();
    // Reused scratch vectors so Spike.update doesn't allocate two Vector2
    // per (spike × contact) every frame. With dense melee the GC churn
    // showed up as noticeable jitter in earlier profiling. Transient +
    // lazy-init: making them transient avoids breaking older saves whose
    // Spike objects didn't have these fields, and the lazy check keeps it
    // safe after deserialisation.
    private transient Vector2 tmpDir;
    private transient Vector2 tmpStart;
    // 10 → 2. dps = attackFactor × (attack - defense). With attackFactor=10
    // a typical attack-defense gap of 3-5 gave dps=30-50, meaning a cell
    // in contact lost 0.03-0.05 health/tick → death in ~20-30 ticks. At
    // 256× time dilation that's microseconds of real time — the user saw
    // it as "instant kill". 2 brings dps to 6-10, so combat takes ~10
    // sim-seconds to kill rather than 0.02 — slow enough that prey can
    // try to flee, defenders have time to take effect, and the
    // SPIKE_DAMAGE death cause stops dominating early-game logs.
    private final float attackFactor = 2f;
    private float lastDPS = 0;
    private float extension = 1;
    private float myLastAttack = 0, theirLastDefense = 0;

    public Spike() {
        super(null);
    }

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

        // Spikes must be ACTIVELY extended to deal damage. Default-state
        // signal (input≈0 from random/un-evolved GRN weights) must give
        // extension≈0. Earlier `clamp(input[0], 0, 1)` still allowed an
        // input of 0.3 → 30% extension → meaningful damage; plants with
        // default GRN signal kept passively killing grazers at a slower
        // rate. Require input > 0.5 before any meaningful extension: this
        // is the "aggression threshold" the cell's NN must actively
        // cross — random weights almost never sustain it, but evolved
        // combat lineages can.
        extension = Functions.clampedLinearRemap(input[0], 0.5f, 1f, 0f, 1f);
        spikePoint = getSpikePoint();

        for (Object toInteract : cell.getInteractionQueue()) {
            if (toInteract instanceof Particle && (((Particle) toInteract).getUserData() instanceof Cell)) {
                Cell other = (Cell) ((Particle) toInteract).getUserData();
                // Plants don't spike other plants. Plants are stationary
                // neighbours that often touch — without this guard, any
                // plant that evolved spikes would damage every adjacent
                // plant, including would-be mutualists for adhesion. The
                // user explicitly didn't want plant-on-plant friendly fire.
                // Protozoa-on-protozoa spikes ARE still allowed (predation
                // / combat), and plant→protozoa defensive damage works as
                // before.
                if (cell instanceof com.protoevo.biology.cells.PlantCell
                        && other instanceof com.protoevo.biology.cells.PlantCell)
                    continue;
                // Don't spike adhered partners — without this, any cluster
                // that evolved adhesion + spikes would tear itself apart, so
                // multicellular defensive structures couldn't emerge. Cells
                // don't recognize bound friends; this rule lets them.
                if (cell.isAttachedTo(other))
                    continue;
                if (tmpDir == null) tmpDir = new Vector2();
                if (tmpStart == null) tmpStart = new Vector2();
                Vector2 nodePos = node.getWorldPosition();
                tmpDir.set(spikePoint).sub(nodePos);
                tmpStart.set(nodePos).sub(other.getPos());
                float[] ts = Geometry.circleIntersectLineTs(tmpDir, tmpStart, other.getRadius());
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
                        // Spikes were free attacks. Charge a small basal
                        // energy cost so combat isn't free, but don't make
                        // it ruinous — earlier `dps * delta * 5f` came out
                        // to ~250 J/sec for a typical attack, draining the
                        // attacker in under 6 sim-seconds. New rate is
                        // ~5 J/sec sustained — feels like real metabolism
                        // overhead without being self-defeating.
                        float energyCost = dps * delta * 0.1f;
                        cell.depleteEnergy(energyCost);
                        cell.addActivity(0.1f * delta);
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
