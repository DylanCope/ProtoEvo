package com.protoevo.biology.nn;

import com.badlogic.gdx.math.MathUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.utils.Utils;

import java.io.Serializable;

public class NeuronGene implements Comparable<NeuronGene>, Serializable
{
    private final long signature;
    private final int id;
    private final Neuron.Type type;
    private ActivationFn activation;

    private String label;
    @JsonIgnore
    private Object[] tags;
    private boolean disabled;
    private float mutationRate = Environment.settings.evo.globalMutationChance.get();
    private float mutationRateMin = Environment.settings.evo.minMutationChance.get();
    private float mutationRateMax = Environment.settings.evo.maxMutationChance.get();
    private int nMutations = 0;
    private int nMutationRateMutations = 0;
    @JsonIgnore
    private int genomeIdx = -1;

    public NeuronGene(int id, Neuron.Type type, ActivationFn activation)
    {
        this(id, type, activation, null);
    }

    public NeuronGene(NeuronGene other) {
        this.signature = other.signature;
        this.id = other.id;
        this.type = other.type;
        this.activation = other.activation;
        this.label = other.label;
        this.tags = other.tags;
        this.disabled = other.disabled;
        this.mutationRate = other.mutationRate;
        this.mutationRateMin = other.mutationRateMin;
        this.mutationRateMax = other.mutationRateMax;
        this.nMutations = other.nMutations;
        this.nMutationRateMutations = other.nMutationRateMutations;
    }

    public NeuronGene(int id, Neuron.Type type, ActivationFn activation, String label)
    {
        signature = Utils.randomLong();
        this.id = id;
        this.type = type;
        this.activation = activation;
        this.label = label;
        disabled = false;
    }

    public void setMutationRange(float min, float max) {
        mutationRateMin = min;
        mutationRateMax = max;
        mutationRate = MathUtils.random(min, max);
    }

    public float getMinMutationRate() {
        return mutationRateMin;
    }

    public float getMaxMutationRate() {
        return mutationRateMax;
    }

    public float getMutationRate() {
        return mutationRate;
    }

    public int getNumMutations() {
        return nMutations;
    }

    public NeuronGene cloneWithMutation() {
        NeuronGene newGene = new NeuronGene(this);
        if (Math.random() > mutationRate)
            return newGene;

        nMutations++;

        if (type == Neuron.Type.HIDDEN)
            newGene.activation = ActivationFn.randomActivation();

        if (Simulation.RANDOM.nextBoolean()) {
            newGene.mutationRate = MathUtils.random(mutationRateMin, mutationRateMax);
            nMutationRateMutations++;
        }

        if (Math.random() < Environment.settings.evo.deleteNeuronMutationRate.get() && type == Neuron.Type.HIDDEN)
            newGene.disable();

        return newGene;
    }

    @Override
    public int compareTo(NeuronGene o) {
        return id - o.id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NeuronGene)
            return ((NeuronGene) o).getId() == id;
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        String str = String.format("Neuron: id=%d; type=%s", id, type);
        if (label != null)
            str += ", label=" + label;
        return str;
    }

    public void disable() {
        disabled = true;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public int getId() {
        return id;
    }

    public Neuron.Type getType() {
        return type;
    }

    public ActivationFn getActivation() {
        return activation;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setTags(Object...tags) {
        this.tags = tags;
    }

    public Object[] getTags() {
        return tags;
    }

    public boolean isGenomeIdxKnown() {
        return genomeIdx != -1;
    }

    public int getGenomeIdx() {
        return genomeIdx;
    }

    public void setGenomeIdx(int myIdx) {
        genomeIdx = myIdx;
    }

    public int getMutationCount() {
        return nMutations + nMutationRateMutations;
    }
}
