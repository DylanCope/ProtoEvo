package com.protoevo.biology.nn;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.protoevo.env.Environment;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by Dylan on 26/05/2017.
 */

@JsonIdentityInfo(
        generator = ObjectIdGenerators.IntSequenceGenerator.class,
        scope = Environment.class)
public class Neuron implements Comparable<Neuron>, Serializable {

    public enum Type implements Serializable {
        SENSOR("SENSOR"), HIDDEN("HIDDEN"), OUTPUT("OUTPUT");

        private final String value;
        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final long serialVersionUID = 1L;

    private final Neuron[] inputs;
    private final float[] weights;
    private Type type;
    private int id;
    private float state = 0, lastState = 0, nextState = 0;
    private float learningRate = 0;
    private ActivationFn activation;
    private int depth = -1;
    private float graphicsX = -1;
    private float graphicsY = -1;
    private boolean connectedToOutput = true;
    private final String label;
    private Object[] tags;
    private boolean active;

    public Neuron(int id, Neuron[] inputs, float[] weights, Type type, ActivationFn activation, String label)
    {
        this.id = id;
        this.inputs = inputs;
        this.weights = weights;
        this.type = type;
        this.activation = activation;
        this.label = label;

        if (type.equals(Type.OUTPUT))
            connectedToOutput = true;
    }

    void tick()
    {
        nextState = 0.0f;
        for (int i = 0; i < inputs.length; i++)
            nextState += inputs[i].getState() * weights[i];
        nextState = activation.apply(nextState);
    }

    void update()
    {
        lastState = state;
        state = nextState;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Neuron)
            return ((Neuron) o).getId() == id;
        return false;
    }

    public int getId() {
        return id;
    }

    public void setId(int i) {
        id = i;
    }

    public float getState() {
        return state;
    }

    public float getLastState() {
        return lastState;
    }

    public Neuron setState(float s) {
        state = activation.apply(s);
        return this;
    }

    public Neuron setActivation(ActivationFn activation) {
        this.activation = activation;
        return this;
    }

    public void deactivate() {
        active = false;
    }

    public void activate() {
        active = true;
    }

    public boolean isInput(Neuron neuron) {
        for (Neuron n : inputs)
            if (n.equals(neuron))
                return true;
        return false;
    }


    public ActivationFn getActivation() {
        return activation;
    }

    public Neuron[] getInputs() {
        return inputs;
    }

    public float[] getWeights() {
        return weights;
    }

    private float getLearningRate() {
        return learningRate;
    }

    private Neuron setLearningRate(float lr) {
        this.learningRate = lr;
        return this;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public int compareTo(Neuron o) {
        return Comparator.comparingInt(Neuron::getId).compare(this, o);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder(String.format("id:%d, state:%.1f", id, state));
        if (label != null)
            s.append(", label: ").append(label);
        s.append(", connections: [");
        for (int i = 0; i < weights.length; i++)
            s.append(String.format("(%d, %.1f)", i, weights[i]));
        s.append("]");
        return s.toString();
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isConnectedToOutput() {
        return connectedToOutput;
    }

    public void setConnectedToOutput(boolean connectedToOutput) {
        this.connectedToOutput = connectedToOutput;
    }

    public void setGraphicsPosition(float x, float y) {
        graphicsX = x;
        graphicsY = y;
    }

    public float getGraphicsX() {
        return graphicsX;
    }

    public float getGraphicsY() {
        return graphicsY;
    }

    public String getLabel() {
        return label == null ? "Neuron " + id : label;
    }

    public boolean hasLabel() {
        return label != null;
    }

    public void setTags(Object[] tags) {
        this.tags = tags;
    }

    public Object[] getTags() {
        return tags;
    }
}
