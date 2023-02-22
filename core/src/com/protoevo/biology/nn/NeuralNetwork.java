package com.protoevo.biology.nn;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NeuralNetwork implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final Neuron[] outputNeurons;
    private final Neuron[] inputNeurons;
    private final float[] outputs;
    private final Neuron[] neurons;
    private final HashSet<String> outputLabels = new HashSet<>(), inputLabels = new HashSet<>();
    private final int depth;
    private final int nInputs;
    private boolean computedGraphics = false;
    private float nodeSpacing;

    public NeuralNetwork(Neuron[] neurons) {
        this.neurons = neurons;

        int nSensors = 0;
        int nOutputs = 0;
        for (Neuron neuron : neurons) {
            if (neuron == null)
                throw new IllegalArgumentException("Cannot handle null neurons.");
            else if (neuron.getType().equals(Neuron.Type.SENSOR))
                nSensors++;
            else if (neuron.getType().equals(Neuron.Type.OUTPUT))
                nOutputs++;
        }
        this.nInputs = nSensors;

        inputNeurons = new Neuron[nInputs];
        int i = 0;
        for (Neuron neuron : neurons)
            if (neuron.getType().equals(Neuron.Type.SENSOR)) {
                inputNeurons[i] = neuron;
                i++;
            }

        outputNeurons = new Neuron[nOutputs];
        i = 0;
        for (Neuron neuron : neurons)
            if (neuron.getType().equals(Neuron.Type.OUTPUT)) {
                outputNeurons[i] = neuron;
                i++;
            }

        outputs = new float[nOutputs];
        Arrays.fill(outputs, 0f);

        depth = calculateDepth();
    }

    public int getDepth() {
        return depth;
    }

    public int calculateDepth() {
        boolean[] visited = new boolean[neurons.length];
//        int depth = calculateDepth(outputNeurons, visited);
        int depth = 1;
        for (Neuron n : outputNeurons) {
            Arrays.fill(visited, false);
            depth = Math.max(depth, computeDepth(n, visited));
        }

        for (Neuron n : outputNeurons)
            n.setDepth(depth);

        for (Neuron n : inputNeurons)
            n.setDepth(0);

        for (Neuron n : neurons)
            if (n.getDepth() == -1)
                n.setDepth(depth);

        return depth;
    }

    private int computeDepth(Neuron neuron, boolean[] visited) {
        if (neuron.getDepth() != -1)
            return neuron.getDepth();

        visited[neuron.getId()] = true;
        int maxDepth = 0;
        for (Neuron input : neuron.getInputs()) {
            if (visited[input.getId()])
                continue;

            int depth = computeDepth(input, visited);
            maxDepth = Math.max(maxDepth, depth);
        }
        neuron.setDepth(maxDepth + 1);
        return maxDepth + 1;
    }

    private int calculateDepth(Neuron[] explore, boolean[] visited) {

        List<Neuron> unexplored = Arrays.stream(explore)
                .filter(n -> !visited[n.getId()])
                .collect(Collectors.toList());

        for (Neuron n : explore)
            visited[n.getId()] = true;

        int maxDepth = 0;

        for (Neuron n : unexplored) {
            int neuronDepth = 1 + calculateDepth(n.getInputs(), visited);
            n.setDepth(neuronDepth);
            maxDepth = Math.max(maxDepth, neuronDepth);
        }

        return maxDepth;
    }

    public void setInput(float ... values) {
        for (int i = 0; i < values.length; i++)
            inputNeurons[i].setState(values[i]);
    }

    public void tick()
    {
        for (Neuron n : neurons) n.tick();
        for (Neuron n : neurons) n.update();
    }

    public float[] outputs()
    {
        for (int i = 0; i < outputNeurons.length; i++)
            outputs[i] = outputNeurons[i].getState();
        return outputs;
    }

    @Override
    public String toString()
    {
        return Stream.of(neurons)
                .map(Neuron::toString)
                .collect(Collectors.joining("\n"));
    }

    public int getInputSize() {
        return nInputs;
    }

    public int getSize() {
        return neurons.length;
    }

    public Neuron[] getNeurons() {
        return neurons;
    }

    public Neuron[] getInputNeurons() {
        return inputNeurons;
    }

    public Neuron[] getOutputNeurons() {
        return outputNeurons;
    }

    public boolean hasComputedGraphicsPositions() {
        return computedGraphics;
    }

    public void setComputedGraphicsPositions(boolean computedGraphics) {
        this.computedGraphics = computedGraphics;
    }

    public void setGraphicsNodeSpacing(float nodeSpacing) {
        this.nodeSpacing = nodeSpacing;
    }

    public float getGraphicsNodeSpacing() {
        return nodeSpacing;
    }

    public void disableInputsFrom(int i) {
        for (int idx = i; idx < inputNeurons.length; idx++)
            inputNeurons[idx].setConnectedToOutput(false);
//        disableOnlyConnectedToDisabled();
    }

    private void disableOnlyConnectedToDisabled() {
        boolean check = true;
        while (check) {
            check = false;
            for (Neuron neuron : neurons) {
                if (!neuron.isConnectedToOutput())
                    continue;

                boolean allInputsDisabled = true;
                for (Neuron input : neuron.getInputs())
                    if (!input.isConnectedToOutput()) {
                        allInputsDisabled = false;
                        break;
                    }
                if (allInputsDisabled) {
                    neuron.setConnectedToOutput(false);
                    check = true;
                }
            }
        }
    }

    public boolean hasSensor(String label) {
        if (inputLabels.contains(label))
            return true;

        for (Neuron n : neurons) {
            if (n.hasLabel() && n.getLabel().equals(label)) {
                inputLabels.add(label);
            }
        }

        return inputLabels.contains(label);
    }

    public void setInput(String label, float value) {
        for (Neuron n : neurons)
            if (n.hasLabel() && n.getLabel().equals(label)) {
                n.setState(value);
                return;
            }
    }

    public float getOutput(String label) {
        for (Neuron n : neurons)
            if (n.getLabel().equals(label))
                return n.getState();
        throw new RuntimeException("Asked for value of neuron that does not exist: " + label);
    }

    public boolean hasOutput(String outputName) {
        if (outputLabels.contains(outputName))
            return true;

        for (Neuron n : neurons) {
            if (n.hasLabel() && n.getLabel().equals(outputName)) {
                outputLabels.add(outputName);
            }
        }

        return outputLabels.contains(outputName);
    }
}
