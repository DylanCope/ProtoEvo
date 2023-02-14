package com.protoevo.biology.nn;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Created by Dylan on 26/05/2017.
 */
public class Neuron implements Comparable<Neuron>, Serializable {

    public interface Activation extends Function<Float, Float>, Serializable {
        Activation SIGMOID = z -> 1 / (1 + (float) Math.exp(-z));
        Activation LINEAR = z -> z;
        Activation TANH = z -> (float) Math.tanh(z);
        Activation STEP = z -> z > 0 ? 1f : 0f;
        Activation RELU = z -> z > 0 ? z : 0f;

        Activation[] activationFunctions = new Activation[] {
                SIGMOID, LINEAR, TANH, STEP, RELU
        };

        static String toString(Activation activation) {
            if (activation.equals(SIGMOID))
                return "Sigmoid";
            if (activation.equals(LINEAR))
                return "Linear";
            if (activation.equals(TANH))
                return "Tanh";
            return null;
        }
    }

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
    private final int id;
    private float state = 0, lastState = 0, nextState = 0;
    private float learningRate = 0;
    private Activation activation;
    private int depth = -1;
    private float graphicsX = -1;
    private float graphicsY = -1;
    private boolean connectedToOutput = true;
    private final String label;
    private Object[] tags;

    public Neuron(int id, Neuron[] inputs, float[] weights, Type type, Activation activation, String label)
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

    public Neuron setActivation(Activation activation) {
        this.activation = activation;
        return this;
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
        return label;
    }

    public void setTags(Object[] tags) {
        this.tags = tags;
    }

    public Object[] getTags() {
        return tags;
    }
}
