package com.protoevo.settings;

public class EvolutionSettings extends Settings {
    public final Parameter<Float> globalMutationChance = new Parameter<>(
            "Global Mutation Chance",
            "The fallback mutation chance for all mutations when there is no local choice.",
            0.01f);
    public final Parameter<Integer> initialGRNMutations = new Parameter<>(
            "Initial GRN Mutations",
            "The number of mutations to apply to the initial gene regulatory network (GRN).",
            10);
    public final Parameter<Float> initialGenomeConnectivity = new Parameter<>(
            "Initial Genome Connectivity",
            "The initial connectivity of a protozoan's genome.",
            0.5f);
    public final Parameter<Integer> maxGRNSize = new Parameter<>(
            "Max GRN Size",
            "The maximum number of hidden nodes in a GRN.",
            16);
    public final Parameter<Float> structuralMutationChance = new Parameter<>(
            "Global Mutation Chance",
            "The fallback mutation chance for all mutations when there is no local choice.",
            0.1f);
    public final Parameter<Float> minMutationChance = new Parameter<>(
            "Minimum Mutation Chance",
            "The minimum mutation chance for all mutations.",
            0.0001f);
    public final Parameter<Float> maxMutationChance = new Parameter<>(
            "Maximum Mutation Chance",
            "Maximum mutation chance for all mutations.",
            0.05f);
    public final Parameter<Float> minTraitMutationChance = new Parameter<>(
            "Min Trait Mutation Chance",
            "",
            0.001f);
    public final Parameter<Float> maxTraitMutationChance = new Parameter<>(
            "Max Trait Mutation Chance",
            "",
            0.05f);
    public final Parameter<Float> minRegulationMutationChance = new Parameter<>(
            "Min Regulation Mutation Chance",
            "",
            0.001f);
    public final Parameter<Float> maxRegulationMutationChance = new Parameter<>(
            "Max Regulation Mutation Chance",
            "",
            0.05f);
    public final Parameter<Double> deleteSynapseMutationRate = new Parameter<>(
            "Delete Synapse Mutation Rate",
            "The chance that a synapse will be deleted when mutating a cell.",
            0.1);
    public final Parameter<Double> deleteNeuronMutationRate = new Parameter<>(
            "Delete Neuron Mutation Rate",
            "The chance that a neuron will be deleted when mutating a cell.",
            0.1);

    public EvolutionSettings() {
        super("Evolution");
    }
}
