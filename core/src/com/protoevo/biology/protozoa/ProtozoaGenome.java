package com.protoevo.biology.protozoa;

import java.io.Serializable;

/**
 * Created by dylan on 28/05/2017.
 */
public class ProtozoaGenome implements Serializable
{
//    public static final long serialVersionUID = 2421454107847378624L;
//    private final Gene<?>[] genes;
//    private float mutationChance = Settings.globalMutationChance;
//    public static final int actionSpaceSize = 3;
//    public static final int nonVisualSensorSize = 3;
//
//    private int parent1Hash = 0;
//    private int parent2Hash = 0;
//
//    public ProtozoaGenome(ProtozoaGenome parentGenome) {
//        mutationChance = parentGenome.mutationChance;
//        genes = Arrays.copyOf(parentGenome.genes, parentGenome.genes.length);
//        parent1Hash = parentGenome.hashCode();
//    }
//
//    public ProtozoaGenome()
//    {
//        NetworkGenome networkGenome = new NetworkGenome();
//        networkGenome.addOutput("Turn Amount");
//        networkGenome.addOutput("Speed");
//        networkGenome.addOutput("Mate Desire");
//        networkGenome.addOutput("Attack");
//
//        networkGenome.addSensor("Bias");
//        networkGenome.addSensor("Health");
//        networkGenome.addSensor("Size");
//        networkGenome.addSensor("Mass Available");
//        for (int i = 0; i < Settings.numContactSensors; i++)
//            networkGenome.addSensor("Contact Sensor " + i);
//        if (Settings.enableChemicalField) {
//            networkGenome.addSensor("Pheromone Gradient X");
//            networkGenome.addSensor("Pheromone Gradient Y");
//            networkGenome.addSensor("Pheromone Amount");
//        }
//
//        genes = new Gene<?>[]{
//                new NetworkGene(networkGenome),
//                new ProtozoaColorGene(),
//                new RetinaSizeGene(),
//                new ProtozoaFOVGene(),
//                new ProtozoaGrowthRateGene(),
//                new ProtozoaMaxTurnGene(),
//                new ProtozoaRadiusGene(),
////                new ProtozoaSpikesGene(),
//                new ProtozoaSplitRadiusGene(),
//                new HerbivoreFactorGene(),
//                new RetinalProductionGene(),
//                new CAMProductionGene()
//        };
//
//        ensureCorrectness();
//    }
//
//    public Gene<?>[] getGenes() {
//        return genes;
//    }
//
//    public static int expectedNetworkInputSize(int retinaSize) {
//        int chemicalGradientInputs = Settings.enableChemicalField ? 3 : 0;
//        return 3 * retinaSize
//                + nonVisualSensorSize
//                + chemicalGradientInputs
//                + 1 + Settings.numContactSensors;
//    }
//
//    public ProtozoaGenome(Gene<?>[] genes) {
//        this.genes = genes;
//        NetworkGenome networkGenome = getGeneValue(NetworkGene.class);
//        if (networkGenome != null)
//            ensureCorrectness();
//    }
//
//    public ProtozoaGenome mutate() {
//        Gene<?>[] newGenes = Arrays.copyOf(genes, genes.length);
//        int numMutations = 0;
//        for (int i = 0; i < genes.length; i++) {
//            if (Simulation.RANDOM.nextDouble() < mutationChance) {
//                newGenes[i] = genes[i].mutate(newGenes);
////            } if (genes[i].canDisable() && Simulation.RANDOM.nextDouble() < Settings.globalMutationChance) {
////                newGenes[i] = genes[i].toggle();
//                numMutations += 1;
//            } else {
//                newGenes[i] = genes[i];
//            }
//        }
//        ProtozoaGenome mutatedGenome = new ProtozoaGenome(newGenes);
//        mutatedGenome.parent1Hash = parent1Hash;
//        mutatedGenome.parent2Hash = parent2Hash;
//        return mutatedGenome.ensureCorrectness();
//    }
//
//    public ProtozoaGenome ensureCorrectness() {
//        int retinaSize = getGeneValue(RetinaSizeGene.class);
//        getGeneValue(NetworkGene.class).ensureRetinaSensorsExist(retinaSize);
//        return this;
//    }
//
//    public ProtozoaGenome crossover(ProtozoaGenome other) {
//        Gene<?>[] newGenes = Arrays.copyOf(genes, genes.length);
//        for (int i = 0; i < genes.length; i++)
//            newGenes[i] = genes[i].crossover(other.genes[i]);
//        return new ProtozoaGenome(newGenes).ensureCorrectness();
//    }
//
//    public <T> T getGeneValue(Class<? extends Gene<T>> clazz) {
//        for (Gene<?> gene : genes)
//            if (clazz.isInstance(gene)) {
//                return gene.isDisabled() ?
//                        clazz.cast(gene).disabledValue() :
//                        clazz.cast(gene).getValue();
//            }
//        return null;
//    }
//
//    public Brain brain() throws MiscarriageException {
//        float maxTurn = getMaxTurn();
//        NetworkGenome networkGenome = getGeneValue(NetworkGene.class);
//        if (networkGenome == null)
//            return Brain.EMPTY;
//
//        try {
//            NeuralNetwork nn = networkGenome.phenotype();
//            int expInpSize = expectedNetworkInputSize(retina().numberOfCells());
//            if (nn.getInputSize() < expInpSize)
//                throw new MiscarriageException();
//            else if (nn.getInputSize() > expInpSize) {
//                nn.disableInputsFrom(expInpSize);
//            }
//            return new NNBrain(nn);
//        } catch (IllegalArgumentException e) {
//            throw new MiscarriageException();
//        }
//    }
//
//    public Retina retina()
//    {
//        int retinaSize = getGeneValue(RetinaSizeGene.class);
//        float fov = getGeneValue(ProtozoaFOVGene.class);
//        return new Retina(retinaSize, fov);
//    }
//
//    public float getFloatGeneValue(Class<? extends Gene<Float>> clazz) {
//        return getGeneValue(clazz);
//    }
//
//    public float getRadius()
//    {
//        return getFloatGeneValue(ProtozoaRadiusGene.class);
//    }
//
//    public float getGrowthRate() {
//        return getFloatGeneValue(ProtozoaGrowthRateGene.class);
//    }
//
//    public float getSplitRadius() {
//        return getFloatGeneValue(ProtozoaSplitRadiusGene.class);
//    }
//
//
//    public Protozoan phenotype(Tank tank) throws MiscarriageException
//    {
//        return new Protozoan(this, tank);
//    }
//
//    public Protozoan createChild(Tank tank) throws MiscarriageException {
//        ProtozoaGenome childGenome = new ProtozoaGenome(this);
//        return childGenome.mutate().phenotype(tank);
//    }
//
//    public Protozoan createChild(Tank tank, ProtozoaGenome otherGenome) throws MiscarriageException {
//        if (otherGenome == null)
//            return createChild(tank);
//        tank.registerCrossoverEvent();
//        ProtozoaGenome childGenome = crossover(otherGenome);
//        childGenome.parent1Hash = hashCode();
//        childGenome.parent2Hash = otherGenome.hashCode();
//        return childGenome.mutate().phenotype(tank);
//    }
//
//    public Color getColour() {
//        return getGeneValue(ProtozoaColorGene.class);
//    }
//
//    public int getNumMutations() {
//        int numMutations = 0;
//        for (Gene<?> gene : genes)
//            numMutations += gene.getNumMutations();
//        return numMutations;
//    }
//
////    public Protozoan.Spike[] getSpikes() {
////        return getGeneValue(ProtozoaSpikesGene.class);
////    }
//
//    public float getMaxTurn() {
//        return getFloatGeneValue(ProtozoaMaxTurnGene.class);
//    }
//
//    public float getHerbivoreFactor() {
//        return getGeneValue(HerbivoreFactorGene.class);
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder genomeStr = new StringBuilder();
//        genomeStr.append(parent1Hash).append(",");
//        genomeStr.append(parent2Hash).append(",");
//        genomeStr.append(hashCode()).append(",");
//        for (Gene<?> gene : genes)
//            genomeStr.append(gene.toString()).append(",");
//        return genomeStr.toString();
//    }
}
