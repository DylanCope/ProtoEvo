package com.protoevo.biology.nn;


import com.badlogic.gdx.math.MathUtils;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.utils.Utils;

import java.io.Serializable;
import java.util.Objects;

public class SynapseGene implements Comparable<SynapseGene>, Serializable
{
    private final long signature;
    private static int globalInnovation = 0;
    private final int innovation;
    private NeuronGene in, out;
    private float weight;
    private boolean disabled;
    private float mutationRate = Environment.settings.evo.globalMutationChance.get();
    private float mutationRateMin = Environment.settings.evo.minMutationChance.get();
    private float mutationRateMax = Environment.settings.evo.maxMutationChance.get();
    private int nMutations, nMutationRateMutations;

    public SynapseGene(NeuronGene in, NeuronGene out, float weight, int innovation) {
        this.in = in;
        this.out = out;
        disabled = false;
        this.weight = weight;
        this.innovation = innovation;
        signature = Utils.randomLong();
    }

    public SynapseGene(SynapseGene other) {
        this.in = other.in;
        this.out = other.out;
        this.weight = other.weight;
        this.innovation = other.innovation;
        this.disabled = other.disabled;
        this.mutationRate = other.mutationRate;
        this.mutationRateMin = other.mutationRateMin;
        this.mutationRateMax = other.mutationRateMax;
        this.nMutations = other.nMutations;
        this.nMutationRateMutations = other.nMutationRateMutations;
        this.signature = other.signature;
    }

    public SynapseGene(NeuronGene in, NeuronGene out, float weight) {
        this(in, out, weight, globalInnovation++);

        setMutationRange(
                Math.min(in.getMinMutationRate(), out.getMinMutationRate()),
                Math.max(in.getMaxMutationRate(), out.getMaxMutationRate()));
    }

    public static float randomInitialWeight() {
        return (float) (2 * Simulation.RANDOM.nextDouble() - 1);
    }

    public SynapseGene(NeuronGene in, NeuronGene out) {
        this(in, out, randomInitialWeight(), globalInnovation++);
    }

    @Override
    public int compareTo(SynapseGene g) {
        return innovation - g.innovation;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SynapseGene) {
            SynapseGene otherSynGene = ((SynapseGene) o);
            NeuronGene otherIn = otherSynGene.in;
            NeuronGene otherOut = otherSynGene.out;
            return in.equals(otherIn)
                    && out.equals(otherOut);
        }
        return false;
    }

    public float getMutationRate() {
        return mutationRate;
    }

    public void setMutationRange(float min, float max) {
        mutationRateMin = min;
        mutationRateMax = max;
        mutationRate = MathUtils.random(min, max);
    }

    public SynapseGene cloneWithMutation() {
        SynapseGene newGene = new SynapseGene(this);
        if (Math.random() > mutationRate)
            return newGene;

        nMutations++;

        newGene.weight = randomInitialWeight();

        if (Simulation.RANDOM.nextBoolean()) {
            newGene.mutationRate = MathUtils.random(mutationRateMin, mutationRateMax);
            nMutationRateMutations++;
        }

        if (Math.random() < Environment.settings.evo.deleteSynapseMutationRate.get())
            newGene.setDisabled(true);

        return newGene;
    }

    @Override
    public int hashCode() {
        return Objects.hash(in.getId(), out.getId());
    }

    @Override
    public String toString()
    {
        return String.format(
                "Synapse: innov=%d; in=%d; out=%d; w=%.5f; disabled=%b",
                innovation, in.getId(), out.getId(), weight, disabled);
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public int getInnovation() {
        return innovation;
    }

    public NeuronGene getIn() {
        return in;
    }

    public void setIn(NeuronGene in) {
        this.in = in;
    }

    public NeuronGene getOut() {
        return out;
    }

    public void setOut(NeuronGene out) {
        this.out = out;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public int getMutationCount() {
        return nMutations + nMutationRateMutations;
    }
}
