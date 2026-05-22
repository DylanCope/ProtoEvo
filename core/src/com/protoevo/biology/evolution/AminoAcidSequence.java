package com.protoevo.biology.evolution;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.core.Simulation;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Fixed-length sequence of amino-acid letters used for lock-and-key surface
 * recognition between phagocytic receptors and their prey. Evolves by point
 * mutation; identity match (0..1) drives the engulf-rate scaling in
 * PhagocyticReceptor + Cell.eat.
 *
 * Why a sequence and not a single float "tag":
 *   A single-float signature would let an entire predator population converge
 *   on one plant signature with a single floating-point mutation, ending the
 *   arms race instantly. A 24/50-character sequence has enough dimensions
 *   that incremental mutation has to actually search — and that search has a
 *   gradient (partial matches give partial digestion rate), so evolution can
 *   climb it instead of needing a lucky hit. Same reasoning as why real
 *   biological recognition uses protein epitopes and not a single tag.
 */
public final class AminoAcidSequence implements Serializable {

    public static final long serialVersionUID = 1L;

    /** Standard 20-letter amino-acid one-letter code. */
    private static final char[] ALPHABET =
            "ACDEFGHIKLMNPQRSTVWY".toCharArray();

    private char[] residues;

    /** No-arg constructor for Kryo deserialization. Kryo needs a way to
     *  instantiate the class via reflection before populating its fields;
     *  without this, loading any save that contains a Protozoan/PlantCell
     *  throws "Class cannot be created (missing no-arg constructor)".
     *  Empty array is fine — Kryo overwrites `residues` immediately. */
    private AminoAcidSequence() {
        this.residues = new char[0];
    }

    public AminoAcidSequence(int length) {
        this.residues = new char[length];
        for (int i = 0; i < length; i++)
            residues[i] = ALPHABET[Simulation.RANDOM.nextInt(ALPHABET.length)];
    }

    public AminoAcidSequence(char[] residues) {
        // Defensive copy so the caller can't mutate our backing array.
        this.residues = Arrays.copyOf(residues, residues.length);
    }

    public AminoAcidSequence(AminoAcidSequence other) {
        this(other.residues);
    }

    public int length() {
        return residues.length;
    }

    public char[] residues() {
        return residues;
    }

    /**
     * Identity fraction with another sequence, in [0, 1].
     * Returns 0 if the other sequence is null or of a different length —
     * different-length sequences are not comparable and should not be
     * considered any kind of match.
     */
    public float identityWith(AminoAcidSequence other) {
        if (other == null || other.residues.length != residues.length)
            return 0f;
        int matches = 0;
        for (int i = 0; i < residues.length; i++) {
            if (residues[i] == other.residues[i]) matches++;
        }
        return (float) matches / residues.length;
    }

    /**
     * Length of the longest run of CONTIGUOUS matching residues with
     * another sequence. Models the "binding domain" semantics of real
     * receptor-ligand recognition: scattered identity doesn't matter, but
     * a stretch of matching residues forms an actual binding interface.
     * Returns 0 if other is null or length-mismatched.
     */
    public int longestContiguousMatch(AminoAcidSequence other) {
        if (other == null || other.residues.length != residues.length)
            return 0;
        int best = 0, cur = 0;
        for (int i = 0; i < residues.length; i++) {
            if (residues[i] == other.residues[i]) {
                cur++;
                if (cur > best) best = cur;
            } else {
                cur = 0;
            }
        }
        return best;
    }

    /**
     * Return a copy with a small number of point mutations applied. Each
     * position is independently flipped to a random alphabet character with
     * probability {@code perResidueRate}. With the defaults this averages
     * ~1 substitution per generation for the 24-mer protozoan signature
     * and ~2 for the 50-mer plant signature — slow enough that lineages
     * preserve their identity across generations, fast enough that an
     * arms-race opponent can keep up.
     */
    public AminoAcidSequence mutated(float perResidueRate) {
        char[] out = Arrays.copyOf(residues, residues.length);
        for (int i = 0; i < out.length; i++) {
            if (MathUtils.random() < perResidueRate) {
                char prev = out[i];
                // Pick a different letter so mutations actually change
                // something (otherwise ~5% of mutations would be no-ops).
                char next;
                do {
                    next = ALPHABET[Simulation.RANDOM.nextInt(ALPHABET.length)];
                } while (next == prev);
                out[i] = next;
            }
        }
        return new AminoAcidSequence(out);
    }

    @Override
    public String toString() {
        return new String(residues);
    }
}
