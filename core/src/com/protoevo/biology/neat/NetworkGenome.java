package com.protoevo.biology.neat;

import com.google.common.collect.Iterators;
import com.protoevo.core.Simulation;
import com.protoevo.settings.SimulationSettings;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetworkGenome implements Serializable
{
	public static final long serialVersionUID = 6145947068527764820L;
	private NeuronGene[] sensorNeuronGenes, outputNeuronGenes, hiddenNeuronGenes;
	private int nNeuronGenes;
	private SynapseGene[] synapseGenes;
	private Random random = Simulation.RANDOM;
	private float mutationChance = SimulationSettings.globalMutationChance;
	private Neuron.Activation defaultActivation = Neuron.Activation.LINEAR;
	private float fitness = 0.0f;
	private int numMutations = 0, nSensors, nOutputs;

	public NetworkGenome(NetworkGenome other) {
		setProperties(other);
	}

	public void setProperties(NetworkGenome other)
	{
		sensorNeuronGenes = other.sensorNeuronGenes;
		outputNeuronGenes = other.outputNeuronGenes;
		hiddenNeuronGenes = other.hiddenNeuronGenes;
		synapseGenes = other.synapseGenes;
		nNeuronGenes = other.nNeuronGenes;
		random = other.random;
		mutationChance = other.mutationChance;
		defaultActivation = other.defaultActivation;
		fitness = other.fitness;
		numMutations = other.numMutations;
		nSensors = other.nSensors;
		nOutputs = other.nOutputs;
	}

	public NetworkGenome() {
		this(0, 0);
	}

	public NetworkGenome(int numInputs, int numOutputs)
	{
		this(numInputs, numOutputs, Neuron.Activation.TANH);
	}

	public NetworkGenome(int numInputs, int numOutputs, Neuron.Activation defaultActivation)
	{
		this.nSensors = numInputs;
		this.nOutputs = numOutputs;

		nNeuronGenes = 0;
		sensorNeuronGenes = new NeuronGene[numInputs];
		for (int i = 0; i < numInputs; i++)
			sensorNeuronGenes[i] = new NeuronGene(nNeuronGenes++, Neuron.Type.SENSOR, Neuron.Activation.LINEAR);

		outputNeuronGenes = new NeuronGene[numOutputs];
		for (int i = 0; i < numOutputs; i++)
			outputNeuronGenes[i] = new NeuronGene(nNeuronGenes++, Neuron.Type.OUTPUT, defaultActivation);

		hiddenNeuronGenes = new NeuronGene[0];

		synapseGenes = new SynapseGene[numInputs * numOutputs];
		for (int i = 0; i < numInputs; i++)
			for (int j = 0; j < numOutputs; j++) {
				NeuronGene in = sensorNeuronGenes[i];
				NeuronGene out = outputNeuronGenes[j];
				synapseGenes[i*numOutputs + j] = new SynapseGene(in, out);
			}

		this.defaultActivation = defaultActivation;
	}

	public NetworkGenome(NeuronGene[] sensorGenes,
						 NeuronGene[] outputGenes,
						 NeuronGene[] hiddenGenes,
						 SynapseGene[] synapseGenes,
						 Neuron.Activation activation) {
		this.sensorNeuronGenes = sensorGenes;
		this.outputNeuronGenes = outputGenes;
		this.hiddenNeuronGenes = hiddenGenes;
		this.synapseGenes = synapseGenes;
		this.defaultActivation = activation;

		nSensors = sensorGenes.length;
		nOutputs = outputGenes.length;
		nNeuronGenes = nSensors + nOutputs + hiddenGenes.length;
	}

	public NeuronGene addSensor(String label, Neuron.Activation activation, Object...tags) {
		NeuronGene n = new NeuronGene(
				nNeuronGenes++, Neuron.Type.SENSOR, activation, label
		);
		n.setTags(tags);

		sensorNeuronGenes = Arrays.copyOf(sensorNeuronGenes, sensorNeuronGenes.length + 1);
		sensorNeuronGenes[sensorNeuronGenes.length - 1] = n;
		nSensors++;

		return n;
	}

	public void addSensor(NeuronGene sensor) {
		sensorNeuronGenes = Arrays.copyOf(sensorNeuronGenes, sensorNeuronGenes.length + 1);
		sensorNeuronGenes[sensorNeuronGenes.length - 1] = sensor;
		nSensors++;
	}

	public NeuronGene addSensor(String label, Object...tags) {
		return addSensor(label, Neuron.Activation.LINEAR, tags);
	}

	public void addFullyConnectedSensor(String label) {
		NeuronGene n = addSensor(label);

		int originalLen = synapseGenes.length;
		synapseGenes = Arrays.copyOf(synapseGenes, originalLen + outputNeuronGenes.length);
		for (int i = 0; i < outputNeuronGenes.length; i++)
			synapseGenes[originalLen + i] = new SynapseGene(n, outputNeuronGenes[i]);
	}

	public NeuronGene addOutput(String label, Neuron.Activation activation, Object...tags) {
		NeuronGene n = new NeuronGene(
				nNeuronGenes++, Neuron.Type.OUTPUT, activation, label
		);
		n.setTags(tags);

		outputNeuronGenes = Arrays.copyOf(outputNeuronGenes, outputNeuronGenes.length + 1);
		outputNeuronGenes[outputNeuronGenes.length - 1] = n;
		nOutputs++;
		return n;
	}

	public NeuronGene addOutput(String label, Object...tags) {
		return addOutput(label, defaultActivation, tags);
	}

	public void addFullyConnectedOutput(String label) {
		NeuronGene n = addOutput(label);

		int originalLen = synapseGenes.length;
		synapseGenes = Arrays.copyOf(synapseGenes, originalLen + sensorNeuronGenes.length);
		for (int i = 0; i < sensorNeuronGenes.length; i++)
			synapseGenes[originalLen + i] = new SynapseGene(sensorNeuronGenes[i], n);
	}

	public void addSynapse(NeuronGene in, NeuronGene out, float w) {
		synapseGenes = Arrays.copyOf(synapseGenes, synapseGenes.length + 1);
		synapseGenes[synapseGenes.length - 1] = new SynapseGene(in, out, w);
	}

	private void createHiddenBetween(SynapseGene g) {

		NeuronGene n = new NeuronGene(
			nNeuronGenes++, Neuron.Type.HIDDEN, defaultActivation
		);

		hiddenNeuronGenes = Arrays.copyOf(hiddenNeuronGenes, hiddenNeuronGenes.length + 1);
		hiddenNeuronGenes[hiddenNeuronGenes.length - 1] = n;

		SynapseGene inConnection = new SynapseGene(g.getIn(), n, 1f);
		SynapseGene outConnection = new SynapseGene(n, g.getOut(), g.getWeight());

		synapseGenes = Arrays.copyOf(synapseGenes, synapseGenes.length + 2);
		synapseGenes[synapseGenes.length - 2] = inConnection;
		synapseGenes[synapseGenes.length - 1] = outConnection;

		g.setDisabled(true);
	}

	private int getSynapseGeneIndex(NeuronGene in, NeuronGene out) {

		for (int i = 0; i < synapseGenes.length - 2; i++) {
			if (synapseGenes[i].getIn().equals(in) &&
					synapseGenes[i].getOut().equals(out) &&
					!synapseGenes[i].isDisabled()) {
				return i;
			}
		}
		return -1;
	}

	private int getNeuronGeneIndex(NeuronGene gene) {
		for (int i = 0; i < sensorNeuronGenes.length; i++)
			if (sensorNeuronGenes[i].equals(gene))
				return i;
		for (int i = 0; i < outputNeuronGenes.length; i++)
			if (outputNeuronGenes[i].equals(gene))
				return i;
		for (int i = 0; i < hiddenNeuronGenes.length; i++)
			if (hiddenNeuronGenes[i].equals(gene))
				return i;
		return -1;
	}

	public NeuronGene getNeuronGene(String name) {
		for (NeuronGene n : sensorNeuronGenes)
			if (n.getLabel().equals(name))
				return n;
		for (NeuronGene n : outputNeuronGenes)
			if (n.getLabel().equals(name))
				return n;
		for (NeuronGene n : hiddenNeuronGenes)
			if (n.getLabel().equals(name))
				return n;
		return null;
	}
	
	private void mutateConnection(NeuronGene in, NeuronGene out) {
		numMutations++;

		int geneIndex = getSynapseGeneIndex(in, out);

		if (geneIndex == -1) {  // synapse doesn't exist - create a new one
			synapseGenes = Arrays.copyOf(synapseGenes, synapseGenes.length + 1);
			synapseGenes[synapseGenes.length - 1] = new SynapseGene(in, out);
		} else {  // synapse does exist
			SynapseGene g = synapseGenes[geneIndex];
			if (random.nextBoolean())   // create new hidden neuron
				createHiddenBetween(g);
			else  // or randomly change the synapse weight
				synapseGenes[geneIndex] = new SynapseGene(in, out, SynapseGene.randomInitialWeight(), g.getInnovation());
		}
	}

	private void mutateSensor(NeuronGene neuronGene) {
		if (hiddenNeuronGenes.length + outputNeuronGenes.length == 0)
			return;
		int otherIdx = random.nextInt(hiddenNeuronGenes.length + outputNeuronGenes.length);
		if (otherIdx < outputNeuronGenes.length)
			mutateConnection(neuronGene, outputNeuronGenes[otherIdx]);
		else
			mutateConnection(neuronGene, hiddenNeuronGenes[otherIdx - outputNeuronGenes.length]);
	}

	private void mutateOutput(NeuronGene neuronGene) {
		if (hiddenNeuronGenes.length + sensorNeuronGenes.length == 0)
			return;
		int otherIdx = random.nextInt(hiddenNeuronGenes.length + sensorNeuronGenes.length);
		if (otherIdx < sensorNeuronGenes.length)
			mutateConnection(sensorNeuronGenes[otherIdx], neuronGene);
		else
			mutateConnection(hiddenNeuronGenes[otherIdx - sensorNeuronGenes.length], neuronGene);
	}

	private void mutateHidden(NeuronGene neuronGene) {
		if (!neuronGene.getType().equals(Neuron.Type.HIDDEN))
			throw new IllegalArgumentException("Neuron must be hidden");

		if (Simulation.RANDOM.nextBoolean() && hiddenNeuronGenes.length + outputNeuronGenes.length > 0) {
			// random connection mutation involving this neuron
			int otherIdx = random.nextInt(
					hiddenNeuronGenes.length +
					outputNeuronGenes.length);
			if (otherIdx < hiddenNeuronGenes.length)
				mutateConnection(neuronGene, hiddenNeuronGenes[otherIdx]);
			else
				mutateConnection(neuronGene, outputNeuronGenes[otherIdx - hiddenNeuronGenes.length]);
		}
		else {  // mutate the node itself (activation function)
			int myIdx = getNeuronGeneIndex(neuronGene);
			hiddenNeuronGenes[myIdx] = neuronGene.mutate();
		}
	}

	public void mutateNeuronGene(NeuronGene neuronGene) {
		numMutations++;

		if (neuronGene.getType() == Neuron.Type.SENSOR) {
			mutateSensor(neuronGene);
			return;
		}

		if (neuronGene.getType() == Neuron.Type.OUTPUT) {
			mutateOutput(neuronGene);
			return;
		}

		mutateHidden(neuronGene);
	}
	
	public void mutate()
	{
		iterateNeuronGenes()
				.forEachRemaining(neuronGene -> {
					if (Math.random() < mutationChance) {
						mutateNeuronGene(neuronGene);
					}
				});
	}
	
	public NetworkGenome crossover(NetworkGenome other)
	{
		Map<Integer, SynapseGene> myConnections = Arrays.stream(synapseGenes)
				.collect(Collectors.toMap(SynapseGene::getInnovation, Function.identity()));
		Map<Integer, SynapseGene> theirConnections = Arrays.stream(other.synapseGenes)
				.collect(Collectors.toMap(SynapseGene::getInnovation, Function.identity()));

		Set<Integer> innovationNumbers = new HashSet<>();
		innovationNumbers.addAll(myConnections.keySet());
		innovationNumbers.addAll(theirConnections.keySet());

		HashSet<SynapseGene> childSynapses = new HashSet<>();

		for (int innovation : innovationNumbers) {
			boolean iContain = myConnections.containsKey(innovation);
			boolean theyContain = theirConnections.containsKey(innovation);
			SynapseGene g;
			if (iContain && theyContain) {
				g = Simulation.RANDOM.nextBoolean() ?
						myConnections.get(innovation) :
						theirConnections.get(innovation);
				if (g.isDisabled() && Simulation.RANDOM.nextFloat() < SimulationSettings.globalMutationChance)
					g.setDisabled(false);
				childSynapses.add(g);
				continue;

			} else if (iContain) {
				g = myConnections.get(innovation);
			} else {
				g = theirConnections.get(innovation);
			}

			if (g.getIn().getType().equals(Neuron.Type.SENSOR) || Simulation.RANDOM.nextBoolean())
				childSynapses.add(g);
		}

		SynapseGene[] childSynapseArray = childSynapses.toArray(new SynapseGene[0]);

		Set<NeuronGene> neuronGenes = childSynapses.stream()
				.flatMap(s -> Stream.of(s.getIn(), s.getOut()))
				.collect(Collectors.toSet());

		NeuronGene[] childSensorGenes = neuronGenes.stream()
				.filter(n -> n.getType().equals(Neuron.Type.SENSOR))
				.sorted(Comparator.comparingInt(NeuronGene::getId))
				.toArray(NeuronGene[]::new);

		NeuronGene[] childOutputGenes = neuronGenes.stream()
				.filter(n -> n.getType().equals(Neuron.Type.OUTPUT))
				.sorted(Comparator.comparingInt(NeuronGene::getId))
				.toArray(NeuronGene[]::new);

		NeuronGene[] childHiddenGenes = neuronGenes.stream()
				.filter(n -> n.getType().equals(Neuron.Type.HIDDEN))
				.sorted(Comparator.comparingInt(NeuronGene::getId))
				.toArray(NeuronGene[]::new);

		return new NetworkGenome(
				childSensorGenes,
				childOutputGenes,
				childHiddenGenes,
				childSynapseArray,
				defaultActivation
		);
	}

	private int maxNeuronId() {
		int id = 0;
		for (NeuronGene g : sensorNeuronGenes)
			id = Math.max(g.getId(), id);
		for (NeuronGene g : hiddenNeuronGenes)
			id = Math.max(g.getId(), id);
		for (NeuronGene g : outputNeuronGenes)
			id = Math.max(g.getId(), id);
		return id;
	}

	public NeuralNetwork phenotype()
	{

		Neuron[] neurons = new Neuron[maxNeuronId() + 1];

		for (NeuronGene g : sensorNeuronGenes) {

			Neuron[] inputs = new Neuron[0];
			float[] weights = new float[0];

			neurons[g.getId()] = new Neuron(
					g.getId(), inputs, weights, g.getType(), g.getActivation(), g.getLabel()
			);
			neurons[g.getId()].setTags(g.getTags());
		}

		int[] inputCounts = new int[neurons.length];
		Arrays.fill(inputCounts, 0);

		for (SynapseGene g : synapseGenes)
			inputCounts[g.getOut().getId()]++;

		for (int i = 0; i < hiddenNeuronGenes.length + outputNeuronGenes.length; i++) {
			NeuronGene g;
			if (i < hiddenNeuronGenes.length)
				g = hiddenNeuronGenes[i];
			else g = outputNeuronGenes[i - hiddenNeuronGenes.length];

			Neuron[] inputs = new Neuron[inputCounts[g.getId()]];
			float[] weights = new float[inputCounts[g.getId()]];

			neurons[g.getId()] = new Neuron(
					g.getId(), inputs, weights, g.getType(), g.getActivation(), g.getLabel()
			);
			neurons[g.getId()].setTags(g.getTags());
		}

		Arrays.fill(inputCounts, 0);
		for (SynapseGene g : synapseGenes) {
			int i = inputCounts[g.getOut().getId()]++;
			neurons[g.getOut().getId()].getInputs()[i] = neurons[g.getIn().getId()];
			neurons[g.getOut().getId()].getWeights()[i] = g.getWeight();
		}

		return new NeuralNetwork(neurons);
	}

	public float distance(NetworkGenome other)
	{
//		int excess = 0;
//		int disjoint = 0;
		return 0;
	}

	public String toString()
	{
		StringBuilder str = new StringBuilder();
		for (NeuronGene gene : sensorNeuronGenes)
			str.append(gene.toString()).append("\n");
		for (NeuronGene gene : hiddenNeuronGenes)
			str.append(gene.toString()).append("\n");
		for (NeuronGene gene : outputNeuronGenes)
			str.append(gene.toString()).append("\n");
		for (SynapseGene gene : synapseGenes)
			str.append(gene.toString()).append("\n");
		return str.toString();
	}

	public SynapseGene[] getSynapseGenes() {
		return synapseGenes;
	}

	public int getNumMutations() {
		return numMutations;
	}

	public int numberOfSensors() {
		return nSensors;
	}

	public boolean hasSensor(String label) {
		for (NeuronGene gene : sensorNeuronGenes)
			if (gene.getLabel().equals(label))
				return true;
		return false;
	}

	public Iterator<NeuronGene> iterateNeuronGenes() {
		return Iterators.concat(
				Iterators.forArray(sensorNeuronGenes),
				Iterators.forArray(hiddenNeuronGenes),
				Iterators.forArray(outputNeuronGenes)
		);
	}

	public void setMutationChance(float mutationChance) {
		this.mutationChance = mutationChance;
	}
}
