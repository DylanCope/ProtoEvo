package com.protoevo.settings;

public class EvolutionSettings extends Settings {
    public final Settings.Parameter<Float> globalMutationChance = new Settings.Parameter<>(
            "Global Mutation Chance",
            "The fallback mutation chance for all mutations when there is no local choice.",
            0.01f);
    public final Settings.Parameter<Integer> initialGRNMutations = new Settings.Parameter<>(
            "Initial GRN Mutations",
            "The number of mutations to apply to the initial gene regulatory network (GRN).",
            10);
    public final Settings.Parameter<Float> initialGenomeConnectivity = new Settings.Parameter<>(
            "Initial Genome Connectivity",
            "The initial connectivity of a protozoan's genome.",
            0.5f);
    public final Settings.Parameter<Integer> maxGRNSize = new Settings.Parameter<>(
            "Max GRN Size",
            "The maximum number of hidden nodes in a GRN.",
            16);
    public final Settings.Parameter<Float> structuralMutationChance = new Settings.Parameter<>(
            "Global Mutation Chance",
            "The fallback mutation chance for all mutations when there is no local choice.",
            0.1f);
    public final Settings.Parameter<Float> minMutationChance = new Settings.Parameter<>(
            "Minimum Mutation Chance",
            "The minimum mutation chance for all mutations.",
            0.0001f);
    public final Settings.Parameter<Float> maxMutationChance = new Settings.Parameter<>(
            "Maximum Mutation Chance",
            "Maximum mutation chance for all mutations.",
            0.05f);
    public final Settings.Parameter<Float> minTraitMutationChance = new Settings.Parameter<>(
            "Min Trait Mutation Chance",
            "",
            0.001f);
    public final Settings.Parameter<Float> maxTraitMutationChance = new Settings.Parameter<>(
            "Max Trait Mutation Chance",
            "",
            0.05f);
    public final Settings.Parameter<Float> minRegulationMutationChance = new Settings.Parameter<>(
            "Min Regulation Mutation Chance",
            "",
            0.001f);
    public final Settings.Parameter<Float> maxRegulationMutationChance = new Settings.Parameter<>(
            "Max Regulation Mutation Chance",
            "",
            0.05f);
    public final Settings.Parameter<Double> deleteSynapseMutationRate = new Settings.Parameter<>(
            "Delete Synapse Mutation Rate",
            "The chance that a synapse will be deleted when mutating a cell.",
            0.1);
    public final Settings.Parameter<Double> deleteNeuronMutationRate = new Settings.Parameter<>(
            "Delete Neuron Mutation Rate",
            "The chance that a neuron will be deleted when mutating a cell.",
            0.1);

    public EvolutionSettings() {
        super("Evolution");
    }
}
