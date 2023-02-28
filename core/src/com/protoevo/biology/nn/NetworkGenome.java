package com.protoevo.biology.nn;

import com.badlogic.gdx.math.MathUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
	@JsonIgnore
	private Random random = Simulation.RANDOM;
	private int numStructuralMutations = 0, nSensors, nOutputs;
	private static int maxSynapseMutationsPerGeneration = 10;
	private static int maxNodeMutationsPerGeneration = 10;

	public NetworkGenome(NetworkGenome other) {
		setProperties(other);
	}

	public void setProperties(NetworkGenome other)
	{
		sensorNeuronGenes = copy(other.sensorNeuronGenes);
		outputNeuronGenes = copy(other.outputNeuronGenes);
		hiddenNeuronGenes = copy(other.hiddenNeuronGenes);
		synapseGenes = copy(other.synapseGenes);
		nNeuronGenes = other.nNeuronGenes;
		random = other.random;
		numStructuralMutations = other.numStructuralMutations;
		nSensors = other.nSensors;
		nOutputs = other.nOutputs;
	}

	public NetworkGenome() {
		this(0, 0);
	}

	private NeuronGene[] copy(NeuronGene[] neuronGenes) {
		NeuronGene[] newGenes = new NeuronGene[neuronGenes.length];
		for (int i = 0; i < neuronGenes.length; i++)
			newGenes[i] = new NeuronGene(neuronGenes[i]);
		return newGenes;
	}

	private SynapseGene[] copy(SynapseGene[] synapseGenes) {
		SynapseGene[] newGenes = new SynapseGene[synapseGenes.length];
		for (int i = 0; i < synapseGenes.length; i++)
			newGenes[i] = new SynapseGene(synapseGenes[i]);
		return newGenes;
	}

	public NetworkGenome(int numInputs, int numOutputs)
	{
		this.nSensors = numInputs;
		this.nOutputs = numOutputs;

		nNeuronGenes = 0;
		sensorNeuronGenes = new NeuronGene[numInputs];
		for (int i = 0; i < numInputs; i++)
			sensorNeuronGenes[i] = new NeuronGene(nNeuronGenes++, Neuron.Type.SENSOR, ActivationFn.LINEAR);

		outputNeuronGenes = new NeuronGene[numOutputs];
		for (int i = 0; i < numOutputs; i++)
			outputNeuronGenes[i] = new NeuronGene(nNeuronGenes++, Neuron.Type.OUTPUT, ActivationFn.LINEAR);

		hiddenNeuronGenes = new NeuronGene[0];

		synapseGenes = new SynapseGene[numInputs * numOutputs];
		for (int i = 0; i < numInputs; i++)
			for (int j = 0; j < numOutputs; j++) {
				NeuronGene in = sensorNeuronGenes[i];
				NeuronGene out = outputNeuronGenes[j];
				synapseGenes[i*numOutputs + j] = new SynapseGene(in, out);
			}
	}

	public NetworkGenome(NeuronGene[] sensorGenes,
						 NeuronGene[] outputGenes,
						 NeuronGene[] hiddenGenes,
						 SynapseGene[] synapseGenes) {
		this.sensorNeuronGenes = sensorGenes;
		this.outputNeuronGenes = outputGenes;
		this.hiddenNeuronGenes = hiddenGenes;
		this.synapseGenes = synapseGenes;

		nSensors = sensorGenes.length;
		nOutputs = outputGenes.length;
		nNeuronGenes = nSensors + nOutputs + hiddenGenes.length;
	}

	public NeuronGene addSensor(String label, ActivationFn activation, Object...tags) {
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
		return addSensor(label, ActivationFn.LINEAR, tags);
	}

	public void addFullyConnectedSensor(String label) {
		NeuronGene n = addSensor(label);

		int originalLen = synapseGenes.length;
		synapseGenes = Arrays.copyOf(synapseGenes, originalLen + outputNeuronGenes.length);
		for (int i = 0; i < outputNeuronGenes.length; i++)
			synapseGenes[originalLen + i] = new SynapseGene(n, outputNeuronGenes[i]);
	}

	public NeuronGene addOutput(String label, ActivationFn activation, Object...tags) {
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
		return addOutput(label, ActivationFn.LINEAR, tags);
	}

	public void addFullyConnectedOutput(String label) {
		NeuronGene n = addOutput(label);

		int originalLen = synapseGenes.length;
		synapseGenes = Arrays.copyOf(synapseGenes, originalLen + sensorNeuronGenes.length);
		for (int i = 0; i < sensorNeuronGenes.length; i++)
			synapseGenes[originalLen + i] = new SynapseGene(sensorNeuronGenes[i], n);
	}

	public SynapseGene addSynapse(NeuronGene in, NeuronGene out, float w) {
		synapseGenes = Arrays.copyOf(synapseGenes, synapseGenes.length + 1);
		synapseGenes[synapseGenes.length - 1] = new SynapseGene(in, out, w);
		return synapseGenes[synapseGenes.length - 1];
	}

	public SynapseGene addSynapse(NeuronGene in, NeuronGene out) {
		return addSynapse(in, out, MathUtils.random(-1f, 1f));
	}

	private void createHiddenBetween(SynapseGene g) {

		NeuronGene n = new NeuronGene(
			nNeuronGenes++, Neuron.Type.HIDDEN, ActivationFn.LINEAR
		);

		n.setMutationRange(
				SimulationSettings.minRegulationMutationChance,
				SimulationSettings.maxRegulationMutationChance);

		hiddenNeuronGenes = Arrays.copyOf(hiddenNeuronGenes, hiddenNeuronGenes.length + 1);
		hiddenNeuronGenes[hiddenNeuronGenes.length - 1] = n;

		SynapseGene inConnection = new SynapseGene(g.getIn(), n, g.getWeight());
		SynapseGene outConnection = new SynapseGene(n, g.getOut(), 1f);


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
		if (gene.isGenomeIdxKnown())
			return gene.getGenomeIdx();

		for (int i = 0; i < sensorNeuronGenes.length; i++) {
			sensorNeuronGenes[i].setGenomeIdx(i);
		}
		for (int i = 0; i < outputNeuronGenes.length; i++) {
			outputNeuronGenes[i].setGenomeIdx(i);
		}
		for (int i = 0; i < hiddenNeuronGenes.length; i++) {
			hiddenNeuronGenes[i].setGenomeIdx(i);
		}

		return gene.getGenomeIdx();
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
		numStructuralMutations++;

		int idx = getSynapseGeneIndex(in, out);

		if (idx == -1) {  // synapse doesn't exist - create a new one
			synapseGenes = Arrays.copyOf(synapseGenes, synapseGenes.length + 1);
			synapseGenes[synapseGenes.length - 1] = new SynapseGene(in, out);
		} else {  // synapse does exist
			SynapseGene g = synapseGenes[idx];
			synapseGenes[idx] = g.cloneWithMutation();
			if (Math.random() < g.getMutationRate())   // create new hidden neuron
				createHiddenBetween(g);
		}
	}

	/**
	 * The only valid kind of sensor mutation is creating a connection to a hidden or output neuron.
	 * @param sensorGene the neuron to mutate
	 */
	private void mutateSensor(NeuronGene sensorGene) {
		int myIdx = getNeuronGeneIndex(sensorGene);
		sensorNeuronGenes[myIdx] = sensorGene.cloneWithMutation();

		if (hiddenNeuronGenes.length + outputNeuronGenes.length == 0)
			return;

		if (Math.random() < sensorGene.getMutationRate()) {
			int otherIdx = MathUtils.random(hiddenNeuronGenes.length + outputNeuronGenes.length - 1);
			if (otherIdx < outputNeuronGenes.length)
				mutateConnection(sensorGene, outputNeuronGenes[otherIdx]);
			else
				mutateConnection(sensorGene, hiddenNeuronGenes[otherIdx - outputNeuronGenes.length]);
		}
	}

	/**
	 * The only valid kind of output mutation is creating a connection to a hidden or sensor neuron.
	 * @param outputGene the neuron to mutate
	 */
	private void mutateOutput(NeuronGene outputGene) {
		int myIdx = getNeuronGeneIndex(outputGene);
		outputNeuronGenes[myIdx] = outputGene.cloneWithMutation();

		if (hiddenNeuronGenes.length + sensorNeuronGenes.length == 0)
			return;

		if (Math.random() < outputGene.getMutationRate()) {
			int otherIdx = MathUtils.random(hiddenNeuronGenes.length + sensorNeuronGenes.length - 1);
			if (otherIdx < sensorNeuronGenes.length)
				mutateConnection(sensorNeuronGenes[otherIdx], outputGene);
			else
				mutateConnection(hiddenNeuronGenes[otherIdx - sensorNeuronGenes.length], outputGene);
		}
	}

	/**
	 * There are two kinds of hidden neuron mutations: creating a connection to any other neuron,
	 * or changing the activation function.
	 * @param hiddenGene the neuron to mutate
	 */
	private void mutateHidden(NeuronGene hiddenGene) {
		int myIdx = getNeuronGeneIndex(hiddenGene);
		hiddenNeuronGenes[myIdx] = hiddenGene.cloneWithMutation();

		if (Math.random() < hiddenGene.getMutationRate()
				&& hiddenNeuronGenes.length + outputNeuronGenes.length > 0) {
			// random connection mutation involving this neuron
			int otherIdx = MathUtils.random(
					hiddenNeuronGenes.length +
					outputNeuronGenes.length - 1);
			if (otherIdx < hiddenNeuronGenes.length)
				mutateConnection(hiddenGene, hiddenNeuronGenes[otherIdx]);
			else
				mutateConnection(hiddenGene, outputNeuronGenes[otherIdx - hiddenNeuronGenes.length]);
		}
	}

	public void mutateNeuronGene(NeuronGene neuronGene) {
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

	public void mutateSynapseGene(int idx) {
		if (Simulation.RANDOM.nextBoolean())
			synapseGenes[idx] = synapseGenes[idx].cloneWithMutation();
		else if (Math.random() < synapseGenes[idx].getMutationRate())
			createHiddenBetween(synapseGenes[idx]);
	}
	
	public void mutate()
	{
		for (int i = 0; i < maxNodeMutationsPerGeneration; i++) {
			int idx = MathUtils.random(
					0, sensorNeuronGenes.length + outputNeuronGenes.length + hiddenNeuronGenes.length - 1);
			if (idx < sensorNeuronGenes.length)
				mutateSensor(sensorNeuronGenes[idx]);
			else if (idx < sensorNeuronGenes.length + outputNeuronGenes.length)
				mutateOutput(outputNeuronGenes[idx - sensorNeuronGenes.length]);
			else
				mutateHidden(hiddenNeuronGenes[idx - sensorNeuronGenes.length - outputNeuronGenes.length]);
		}

		if (synapseGenes.length > 0)
			for (int i = 0; i < maxSynapseMutationsPerGeneration; i++) {
				int idx = MathUtils.random(0, synapseGenes.length - 1);
				mutateSynapseGene(idx);
			}
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
				if (g.isDisabled() && MathUtils.random() < SimulationSettings.globalMutationChance)
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
				childSynapseArray
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

	public int getNumStructuralMutations() {
		return numStructuralMutations;
	}

	public int getMutationCount() {
		int count = getNumStructuralMutations();

		for (NeuronGene gene : sensorNeuronGenes)
			count += gene.getMutationCount();

		for (NeuronGene gene : hiddenNeuronGenes)
			count += gene.getMutationCount();

		for (NeuronGene gene : outputNeuronGenes)
			count += gene.getMutationCount();

		for (SynapseGene gene : synapseGenes)
			count += gene.getMutationCount();

		return count;
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

	public boolean hasOutput(String label) {
		for (NeuronGene gene : outputNeuronGenes)
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

	public Iterator<SynapseGene> iterateSynapseGenes() {
		return Iterators.forArray(synapseGenes);
	}
}
