package com.protoevo.core.repl;

import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.nn.NetworkGenome;
import com.protoevo.biology.nn.NeuralNetwork;
import com.protoevo.biology.nn.Neuron;
import com.protoevo.biology.nn.SynapseGene;
import com.protoevo.core.Simulation;
import com.protoevo.utils.FileIO;
import com.protoevo.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dumps a single protozoa's neural network state to JSON so it can be
 * inspected outside the running sim. The format is intentionally minimal:
 * just neurons (id, label, type) and synapses (in, out, weight, disabled).
 * Enough to reconstruct the matrix and reason about which sensors drive
 * which outputs.
 */
public class DumpGenome extends Command {
    public DumpGenome(REPL repl) {
        super(repl);
    }

    @Override
    public boolean run(String[] args) {
        Simulation sim = repl.getSimulation();
        Protozoan target = null;

        if (args.length >= 2) {
            // Argument can be a cell id or "biggest" for the protozoa
            // with the most living descendants.
            String arg = args[1];
            if (arg.equals("biggest")) {
                target = findBiggestLineageProtozoan(sim);
            } else {
                try {
                    long id = Long.parseLong(arg);
                    for (Cell c : sim.getEnv().getCells()) {
                        if (c.getId() == id && c instanceof Protozoan) {
                            target = (Protozoan) c;
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid id: " + arg);
                    return false;
                }
            }
        }

        // Fallback: just pick any protozoa.
        if (target == null) {
            for (Cell c : sim.getEnv().getCells()) {
                if (c instanceof Protozoan) {
                    target = (Protozoan) c;
                    break;
                }
            }
        }

        if (target == null) {
            System.out.println("No protozoa to dump.");
            return false;
        }

        Dump dump = buildDump(target);
        String fileName = sim.getSaveFolder() + "/genome-"
                + target.getId() + "-" + Utils.getTimeStampString();
        FileIO.writeJson(dump, fileName);
        System.out.println("Wrote: " + fileName + ".json");
        System.out.printf("  protozoan id=%d lineage=%d gen=%d  neurons=%d synapses=%d%n",
                target.getId(), target.getLineageId(), target.getGeneration(),
                dump.neurons.size(), dump.synapses.size());
        return true;
    }

    private Protozoan findBiggestLineageProtozoan(Simulation sim) {
        Protozoan best = null;
        int bestSize = -1;
        for (Cell c : sim.getEnv().getCells()) {
            if (!(c instanceof Protozoan)) continue;
            var rec = sim.getEnv().getLineageRecords().get(c.getLineageId());
            int size = rec != null ? rec.aliveDescendants : 0;
            if (size > bestSize) {
                bestSize = size;
                best = (Protozoan) c;
            }
        }
        return best;
    }

    private Dump buildDump(Protozoan p) {
        NetworkGenome genome = p.getGeneExpressionFunction().getGRNGenome();
        NeuralNetwork phenotype = p.getGeneExpressionFunction().getRegulatoryNetwork();

        Dump d = new Dump();
        d.protozoanId = p.getId();
        d.lineageId = p.getLineageId();
        d.generation = p.getGeneration();
        d.mutationRateMultiplier = genome.getMutationRateMultiplier();

        // Walk genome neurons (genes, not phenotype) for stable labels.
        var it = genome.iterateNeuronGenes();
        while (it.hasNext()) {
            var gene = it.next();
            NeuronDump n = new NeuronDump();
            n.id = gene.getId();
            n.label = gene.getLabel();
            n.type = gene.getType().toString();
            // Pull live activation from the phenotype if available.
            if (phenotype != null) {
                for (Neuron neuron : phenotype.getNeurons()) {
                    if (neuron.getId() == n.id) {
                        n.lastState = neuron.getLastState();
                        break;
                    }
                }
            }
            d.neurons.add(n);
        }
        for (SynapseGene s : genome.getSynapseGenes()) {
            if (s.isDisabled()) continue; // skip pruned connections
            SynapseDump sd = new SynapseDump();
            sd.from = s.getIn().getId();
            sd.to = s.getOut().getId();
            sd.weight = s.getWeight();
            d.synapses.add(sd);
        }
        return d;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"dumpgenome", "dumpnn"};
    }

    @Override
    public String getDescription() {
        return "Dump a protozoa's NN to JSON.";
    }

    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  dumpgenome                 — pick any protozoa");
        System.out.println("  dumpgenome biggest         — protozoa from the largest lineage");
        System.out.println("  dumpgenome <cellId>        — specific cell by id");
        System.out.println("Output JSON is written to the simulation's save folder.");
    }

    // ===== JSON shape =====
    public static class Dump {
        public long protozoanId;
        public long lineageId;
        public int generation;
        public float mutationRateMultiplier;
        public List<NeuronDump> neurons = new ArrayList<>();
        public List<SynapseDump> synapses = new ArrayList<>();
    }
    public static class NeuronDump {
        public int id;
        public String label;
        public String type;
        public float lastState;
    }
    public static class SynapseDump {
        public int from;
        public int to;
        public float weight;
    }
}
