package com.protoevo.biology.nn;

import com.badlogic.gdx.math.MathUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterators;
import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import org.apache.commons.lang3.ArrayUtils;

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
	// Per-lineage mutation rate multiplier. The global node/synapse budgets
	// in EvolutionSettings act as a baseline; each genome scales them by
	// this factor. Itself mutates each generation via a bounded log-Gaussian
	// step, so lineages can evolve their own pace of exploration. Some
	// settle low (stable adaptations, slow drift) and others go high
	// (fast-exploring but risk losing locked-in good circuits).
	private float mutationRateMultiplier = 1f;
	private static final float MIN_MUTATION_RATE_MULT = 0.25f;
	private static final float MAX_MUTATION_RATE_MULT = 4f;

	public NetworkGenome(NetworkGenome other) {
		setProperties(other);
	}

	public NetworkGenome() {
		this(0, 0);
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
		mutationRateMultiplier = other.mutationRateMultiplier;
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

		if (hiddenNeuronGenes.length >= Environment.settings.evo.maxGRNSize.get())
			return;

		NeuronGene n = new NeuronGene(
			nNeuronGenes++, Neuron.Type.HIDDEN, ActivationFn.LINEAR
		);

		n.setMutationRange(
				Environment.settings.evo.minRegulationMutationChance.get(),
				Environment.settings.evo.maxRegulationMutationChance.get());

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
		// Some neurons (e.g. ones created by the no-label NetworkGenome
		// constructor at line 71-75 used by plants/non-protozoa cells) have
		// null labels. Use name.equals(label) so a null on the genome side
		// returns false instead of NPE'ing.
		for (NeuronGene n : sensorNeuronGenes)
			if (name.equals(n.getLabel()))
				return n;
		for (NeuronGene n : outputNeuronGenes)
			if (name.equals(n.getLabel()))
				return n;
		for (NeuronGene n : hiddenNeuronGenes)
			if (name.equals(n.getLabel()))
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
		if (Simulation.RANDOM.nextFloat() < Environment.settings.evo.structuralMutationChance.get())
			synapseGenes[idx] = synapseGenes[idx].cloneWithMutation();
		else if (Math.random() < synapseGenes[idx].getMutationRate())
			createHiddenBetween(synapseGenes[idx]);
	}
	
	public void mutate()
	{
		int baseNode = Environment.settings.evo.nodeMutationsPerGeneration.get();
		int baseSyn  = Environment.settings.evo.synapseMutationsPerGeneration.get();
		int nodeBudget = Math.max(1, Math.round(baseNode * mutationRateMultiplier));
		int synapseBudget = Math.max(1, Math.round(baseSyn  * mutationRateMultiplier));

		for (int i = 0; i < nodeBudget; i++) {
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
			for (int i = 0; i < synapseBudget; i++) {
				int idx = MathUtils.random(0, synapseGenes.length - 1);
				mutateSynapseGene(idx);
			}

		// Drift the mutation rate itself. Log-Gaussian step (multiplicative)
		// so increments and decrements are symmetric in log-space; bounded
		// to prevent runaway in either direction.
		float step = (float) Math.exp(0.15 * Simulation.RANDOM.nextGaussian());
		mutationRateMultiplier *= step;
		if (mutationRateMultiplier < MIN_MUTATION_RATE_MULT)
			mutationRateMultiplier = MIN_MUTATION_RATE_MULT;
		if (mutationRateMultiplier > MAX_MUTATION_RATE_MULT)
			mutationRateMultiplier = MAX_MUTATION_RATE_MULT;
	}

	public float getMutationRateMultiplier() { return mutationRateMultiplier; }
	
	public NetworkGenome crossover(NetworkGenome other)
	{
		Map<Integer, SynapseGene> myConnections = Arrays.stream(synapseGenes)
				.collect(Collectors.toMap(
						SynapseGene::getInnovation,
						Function.identity(),
						(x1, x2) -> random.nextBoolean() ? x1 : x2));
		Map<Integer, SynapseGene> theirConnections = Arrays.stream(other.synapseGenes)
				.collect(Collectors.toMap(
						SynapseGene::getInnovation,
						Function.identity(),
						(x1, x2) -> random.nextBoolean() ? x1 : x2));

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
				if (g.isDisabled() && MathUtils.random() < Environment.settings.evo.globalMutationChance.get())
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

		for (SynapseGene g : synapseGenes) {
			if (!g.isDisabled())
				inputCounts[g.getOut().getId()]++;
		}

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
			if (g.isDisabled())
				continue;
			int inIdx = g.getIn().getId();
			int outIdx = g.getOut().getId();
			if (inIdx >= neurons.length || outIdx >= neurons.length
					|| neurons[outIdx].getInputs().length == 0)
				continue;
			int i = inputCounts[outIdx];
			neurons[outIdx].getInputs()[i] = neurons[inIdx];
			neurons[outIdx].getWeights()[i] = g.getWeight();
			inputCounts[outIdx]++;
		}

		return new NeuralNetwork(neurons);
	}

	/**
	 * Standard NEAT compatibility distance:
	 *   d = c1*E/N + c2*D/N + c3*Wbar
	 * where E = excess genes (innovations past the other genome's max),
	 *       D = disjoint genes (non-matching innovations within range),
	 *       Wbar = mean absolute weight difference of matching genes,
	 *       N   = max(|genes|, 1) for normalization.
	 * Used by the genetic-clusters view to group similar protozoa.
	 */
	public float distance(NetworkGenome other)
	{
		if (other == null) return Float.POSITIVE_INFINITY;
		final float c1 = 1.0f, c2 = 1.0f, c3 = 0.4f;

		SynapseGene[] a = this.synapseGenes;
		SynapseGene[] b = other.synapseGenes;
		if (a == null) a = new SynapseGene[0];
		if (b == null) b = new SynapseGene[0];

		int maxInnovA = -1, maxInnovB = -1;
		java.util.HashMap<Integer, SynapseGene> mapA = new java.util.HashMap<>();
		java.util.HashMap<Integer, SynapseGene> mapB = new java.util.HashMap<>();
		for (SynapseGene g : a) {
			mapA.put(g.getInnovation(), g);
			if (g.getInnovation() > maxInnovA) maxInnovA = g.getInnovation();
		}
		for (SynapseGene g : b) {
			mapB.put(g.getInnovation(), g);
			if (g.getInnovation() > maxInnovB) maxInnovB = g.getInnovation();
		}

		int excess = 0, disjoint = 0, matching = 0;
		float weightDiffSum = 0f;
		int innovCutoff = Math.min(maxInnovA, maxInnovB);

		java.util.HashSet<Integer> allInnov = new java.util.HashSet<>(mapA.keySet());
		allInnov.addAll(mapB.keySet());
		for (int innov : allInnov) {
			boolean inA = mapA.containsKey(innov);
			boolean inB = mapB.containsKey(innov);
			if (inA && inB) {
				matching++;
				weightDiffSum += Math.abs(mapA.get(innov).getWeight() - mapB.get(innov).getWeight());
			} else if (innov > innovCutoff) {
				excess++;
			} else {
				disjoint++;
			}
		}

		int n = Math.max(Math.max(a.length, b.length), 1);
		float wbar = matching > 0 ? weightDiffSum / matching : 0f;
		return c1 * excess / n + c2 * disjoint / n + c3 * wbar;
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
			if (label.equals(gene.getLabel()))
				return true;
		return false;
	}

	public boolean hasOutput(String label) {
		for (NeuronGene gene : outputNeuronGenes)
			if (label.equals(gene.getLabel()))
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

	public void merge(NetworkGenome other) {
		sensorNeuronGenes = ArrayUtils.addAll(sensorNeuronGenes, other.sensorNeuronGenes);
		hiddenNeuronGenes = ArrayUtils.addAll(hiddenNeuronGenes, other.hiddenNeuronGenes);
		outputNeuronGenes = ArrayUtils.addAll(outputNeuronGenes, other.outputNeuronGenes);
		synapseGenes = ArrayUtils.addAll(synapseGenes, other.synapseGenes);
//		for (NeuronGene gene : other.hiddenNeuronGenes)
//			addNeuronGene(gene);
//		for (NeuronGene gene : other.outputNeuronGenes)
//			addNeuronGene(gene);
//		for (SynapseGene gene : other.synapseGenes)
//			addSynapseGene(gene);
	}
}
