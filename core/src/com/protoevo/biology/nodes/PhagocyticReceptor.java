package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.cells.*;
import com.protoevo.biology.evolution.AminoAcidSequence;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.physics.Collision;
import com.protoevo.physics.Particle;


import java.io.Serializable;

public class PhagocyticReceptor extends NodeAttachment implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private final Vector2 tmp = new Vector2();
    protected Cell lastEngulfed;
    private boolean engulfPlant, engulfMeat;

    public PhagocyticReceptor() {
        super(null);
    }

    public PhagocyticReceptor(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        Cell cell = node.getCell();
        if (!(cell instanceof Protozoan)) {
            return;
        }

        if (input.length == 3)
            handleDim3IO(input, output);
        else if (input.length == 1)
            handleDim1IO(input, output);

        tryEngulfContacts();
    }

    protected void tryEngulfContacts() {
        Cell cell = node.getCell();
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

    public void setShouldEngulf(boolean shouldEngulf) {
        this.engulfPlant = shouldEngulf;
        this.engulfMeat = shouldEngulf;
    }

    public void setShouldEngulfPlant(boolean shouldEngulf) {
        this.engulfPlant = shouldEngulf;
    }

    public void setShouldEngulfMeat(boolean shouldEngulf) {
        this.engulfMeat = shouldEngulf;
    }

    public boolean getShouldEngulfPlant() {
        return engulfPlant;
    }

    public boolean getShouldEngulfMeat() {
        return engulfMeat;
    }

    private void handleDim3IO(float[] input, float[] output) {
        Cell cell = node.getCell();
        // Default-engulf semantics: a cell that built a phagocytic receptor
        // engulfs unless the GRN actively *suppresses* it (input < -0.25).
        // The previous threshold `input > 0` meant random/noisy weights
        // produced ~50% engulf rate, and combined with conservative NEAT
        // perturbation many cells never crossed it — they had a working
        // receptor and starved next to plants.
        engulfPlant = input[0] > -0.25f;
        engulfMeat  = input[1] > -0.25f;

        if (!((Protozoan) cell).getEngulfedCells().contains(lastEngulfed))
            lastEngulfed = null;
        if (lastEngulfed != null) {
            output[0] = lastEngulfed.getHealth();
            if (lastEngulfed instanceof PlantCell)
                output[1] = 1f;
            else if (lastEngulfed instanceof MeatCell)
                output[2] = 1f;
        }
    }

    private void handleDim1IO(float[] input, float[] output) {
        Cell cell = node.getCell();
        // Same default-engulf semantics as the dim-3 path above.
        engulfPlant = input[0] > -0.25f;
        engulfMeat = engulfPlant;

        if (!((Protozoan) cell).getEngulfedCells().contains(lastEngulfed))
            lastEngulfed = null;
        if (lastEngulfed != null) {
            output[0] = lastEngulfed.getHealth();
        }
    }

    // CONTIGUOUS-RUN engulf gate: receptor must share a stretch of at least
    // this many consecutive identical residues with the prey signature.
    // Scattered identity doesn't count — this models a real binding domain
    // where the receptor pocket needs a contiguous epitope to grip.
    //
    // Thresholds live in CellSettings (plantEngulfMinRun / protozoaEngulfMinRun)
    // and are ratcheted up monotonically by the homeostat when the population
    // is stable — so the gate starts forgiving and tightens only as lineages
    // prove they can handle it. The previous hard-coded 10/15 was too strict
    // at the start of a run: fresh sequences rarely cleared it, no engulfs
    // happened, no population sustained, and the existing
    // selectionPressureExponent ratchet couldn't fire either. Reading from
    // settings lets both ratchets co-evolve at the same pace.

    public boolean engulfCondition(Cell other) {
        if (other instanceof PlantCell && !engulfPlant)
            return false;
        if (other instanceof MeatCell && !engulfMeat)
            return false;
        if (node.getCell().isAttachedTo(other))
            return false;
        if (!(other.isEdible()
                && correctSizes(other) && notEngulfed(other)
                && closeEnough(other) && roomFor(other)))
            return false;
        // Meat carries no genome; standard checks above are sufficient.
        if (other instanceof MeatCell) return true;
        // Plant/protozoa engulf needs a CONTIGUOUS run of matching residues
        // long enough to count as a real binding region.
        return longestRun(other) >= engulfMinRunFor(other);
    }

    /**
     * Identity fraction between this cell's appropriate key and the prey's
     * signature/identity, in [0, 1]. Used downstream for digestion efficiency
     * scaling. Meat returns 1.0 (no signature applies); missing keys/sigs
     * return 0.0 so callers can short-circuit cleanly.
     */
    public float signatureMatch(Cell other) {
        if (other instanceof MeatCell) return 1f;
        Cell self = node.getCell();
        if (!(self instanceof Protozoan)) return 0f;
        Protozoan me = (Protozoan) self;

        if (other instanceof PlantCell) {
            AminoAcidSequence key = me.getPlantReceptorKey();
            AminoAcidSequence sig = ((PlantCell) other).getSurfaceSignature();
            return key == null || sig == null ? 0f : key.identityWith(sig);
        }
        if (other instanceof Protozoan) {
            AminoAcidSequence key = me.getProtozoaPhagocyticReceptor();
            AminoAcidSequence sig = ((Protozoan) other).getProtozoaReceivingReceptor();
            return key == null || sig == null ? 0f : key.identityWith(sig);
        }
        return 0f;
    }

    /**
     * Longest contiguous run of matching residues between the receptor key
     * and the prey identity. Different code path from signatureMatch
     * because we use this for the binary engulf GATE; signatureMatch is
     * still used for digest-rate scaling.
     */
    public int longestRun(Cell other) {
        Cell self = node.getCell();
        if (!(self instanceof Protozoan)) return 0;
        Protozoan me = (Protozoan) self;

        if (other instanceof PlantCell) {
            AminoAcidSequence key = me.getPlantReceptorKey();
            AminoAcidSequence sig = ((PlantCell) other).getSurfaceSignature();
            return key == null || sig == null ? 0 : key.longestContiguousMatch(sig);
        }
        if (other instanceof Protozoan) {
            AminoAcidSequence key = me.getProtozoaPhagocyticReceptor();
            AminoAcidSequence sig = ((Protozoan) other).getProtozoaReceivingReceptor();
            return key == null || sig == null ? 0 : key.longestContiguousMatch(sig);
        }
        return 0;
    }

    // Hard floor on protozoa-on-protozoa engulf — kin-cannibalism must
    // ALWAYS require a substantial co-evolved binding region, regardless
    // of any setting value (including those loaded from older saves where
    // the default was lower).
    //
    // 24 of 75 (32% of sequence) means the chance of a random pair
    // matching by coincidence is ~ 52 × (1/20)^24 ≈ 5e-31 — completely
    // impossible. Even strong evolutionary pressure takes hundreds of
    // generations to align two sequences this much. Cannibalism is now a
    // late-game evolutionary achievement, not something cells can stumble
    // into in the first few sim-seconds. This is the user-requested
    // "always have some level of continuance" — and a high level at that.
    private static final int PROTOZOA_MIN_RUN_FLOOR = 24;

    private int engulfMinRunFor(Cell other) {
        if (other instanceof PlantCell)
            return Environment.settings.cell.plantEngulfMinRun.get();
        if (other instanceof Protozoan)
            return Math.max(
                    PROTOZOA_MIN_RUN_FLOOR,
                    Environment.settings.cell.protozoaEngulfMinRun.get());
        return 0;
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
        // Was: prey radius < 0.8 × cell radius × progressFactor. That meant
        // prey had to be substantially smaller than the predator — a cell
        // touching a plant the same size as itself couldn't eat it. Bumped
        // to 1.2× so cells can engulf prey up to ~20% larger; combined with
        // roomFor's area gate this still prevents nonsensical "minnow eats
        // whale" scenarios.
        // Also dropped the 2×minRadius lower bound on the predator: with
        // the default-engulf semantics there's no reason to forbid small
        // cells from trying — they'll fail roomFor naturally if the prey
        // doesn't fit.
        return other.getRadius() < progressFactor * cell.getRadius() * 1.2f
                && cell.getRadius() > Environment.settings.minParticleRadius.get();
    }

    private boolean notEngulfed(Cell other) {
        return !((Protozoan) node.getCell()).getEngulfedCells().contains(other);
    }

    private boolean closeEnough(Cell other) {
        // Was: distance from THIS NODE'S world position. That meant a cell
        // touching prey on its left side wouldn't engulf if its receptor
        // node was angled toward the right — even though physically in
        // contact. Now we accept either: the node is close enough OR the
        // cells are physically in contact (centers within sum of radii +
        // engulfRange). This matches the user-visible expectation: "if my
        // cell is touching food, it should eat."
        Cell cell = node.getCell();
        float r = engulfRange();
        Vector2 nodePos = node.getWorldPosition();
        float dNode = other.getRadius() + r;
        if (nodePos.dst2(other.getPos()) < dNode*dNode) return true;
        float dCenter = cell.getRadius() + other.getRadius() + r * 0.5f;
        return cell.getPos().dst2(other.getPos()) < dCenter*dCenter;
    }

    public float engulfRange() {
        return node.getCell().getRadius() * Environment.settings.protozoa.engulfRangeFactor.get();
    }

    public void engulf(Cell cell) {
        lastEngulfed = cell;
        // Digestion efficiency = bindingStrength^(1 + selectionPressureExponent).
        // bindingStrength = longestRun / sequenceLength, so it captures
        // both "how big is the binding region?" (numerator) normalized
        // against the sequence we're matching against (denominator).
        // A 50-residue full-match pair has bindingStrength = 1.0; a 10-of-50
        // contiguous run = 0.2 (and would be at the minimum engulf gate).
        //
        // The exponent is dynamic, ratcheted upward by the homeostat (see
        // Simulation.maybeRatchetSelection). Pressure=0 means efficiency is
        // linear in bindingStrength (gentle). Pressure=3 means it's quartic
        // (harsh — only near-perfect binding gives full rate).
        //
        // Floor 0.02 keeps meat (no signature, returns 1.0) workable.
        float strength = bindingStrength(cell);
        float exponent = 1f + Environment.settings.cell.selectionPressureExponent.get();
        // Floor is the dynamically-ratcheted engulfBaseEfficiency, not a
        // hardcoded 0.02. When base is 1.0 (game start) eff is always 1.0
        // and the receptor system is effectively bypassed. As the homeostat
        // ratchets base down toward 0.02, signature match progressively
        // matters more — low-match engulfs digest at the floor rate, high
        // matches at strength^exponent. Cells whose signatures don't track
        // plant drift gradually lose efficiency to the ratchet.
        float floor = Environment.settings.cell.engulfBaseEfficiency.get();
        float eff = Math.max(floor, (float) Math.pow(strength, exponent));
        cell.setEngulfEfficiency(eff);
        cell.setEngulfer(node.getCell());
        cell.kill(CauseOfDeath.EATEN);
        ((Protozoan) node.getCell()).getEngulfedCells().add(cell);
    }

    /** Binding strength in [0, 1] — longest contiguous match normalized
     *  by sequence length. Meat (no genome) returns 1.0. */
    public float bindingStrength(Cell other) {
        if (other instanceof MeatCell) return 1f;
        if (other instanceof PlantCell || other instanceof Protozoan) {
            int run = longestRun(other);
            int len = (other instanceof PlantCell)
                    ? com.protoevo.biology.evolution.PlantSignatureTrait.LENGTH
                    : com.protoevo.biology.evolution.ProtozoaSignatureTrait.LENGTH;
            return len <= 0 ? 0f : (float) run / (float) len;
        }
        return 0f;
    }

    @Override
    public String getName() {
        return "Phagocytic Receptor";
    }

    @Override
    public String getInputMeaning(int index) {
        if (node.getIODimension() == 1) {
            if (index == 0)
                return "Should Engulf";
        }
        else {
            if (index == 0)
                return "Should Engulf Plant";
            if (index == 1)
                return "Should Engulf Meat";
        }
        return null;
    }

    @Override
    public String getOutputMeaning(int index) {
            if (index == 0)
                return "Last Engulfed Health";
        if (node.getIODimension() == 3) {
            if (index == 1)
                return "Is Last Engulfed Plant";
            if (index == 2)
                return "Is Last Engulfed Meat";
        }
        return null;
    }

    @Override
    public void addStats(Statistics stats) {
        stats.putCount("Engulfed Cells", ((Protozoan) node.getCell()).getEngulfedCells().size());
        stats.putBoolean("Will Engulf Plant?", engulfPlant);
        stats.putBoolean("Will Engulf Meat?", engulfMeat);
    }
}
