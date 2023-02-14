package com.protoevo.biology.nn;

import com.protoevo.core.Simulation;

import java.io.Serializable;
import java.util.Objects;

public class NeuronGene implements Comparable<NeuronGene>, Serializable
{

    private final int id;
    private final Neuron.Type type;
    private final Neuron.Activation activation;

    private String label;
    private Object[] tags;

    public NeuronGene(int id, Neuron.Type type, Neuron.Activation activation)
    {
        this(id, type, activation, null);
    }

    public NeuronGene(int id, Neuron.Type type, Neuron.Activation activation, String label)
    {
        this.id = id;
        this.type = type;
        this.activation = activation;
        this.label = label;
    }

    public NeuronGene mutate() {
        int randomChoiceIdx = Simulation.RANDOM.nextInt(Neuron.Activation.activationFunctions.length);
        Neuron.Activation newActivation = Neuron.Activation.activationFunctions[randomChoiceIdx];
        return new NeuronGene(id, type, newActivation, label);
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
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String str = String.format("Neuron: id=%d; type=%s", id, type);
        if (label != null)
            str += ", label=" + label;
        return str;
    }

    public int getId() { return id; }
    public Neuron.Type getType() { return type; }
    public Neuron.Activation getActivation() { return activation; }

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
}
