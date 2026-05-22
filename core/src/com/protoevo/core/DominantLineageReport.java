package com.protoevo.core;

import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.AminoAcidSequence;
import com.protoevo.biology.evolution.GeneExpressionFunction;
import com.protoevo.biology.nn.NetworkGenome;
import com.protoevo.biology.nn.NeuronGene;
import com.protoevo.biology.nn.SynapseGene;
import com.protoevo.env.Environment;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Writes a human-readable summary of the currently-dominant protozoan
 * lineage to disk on every save. The point: lets a human (or me, in a
 * later session) inspect what evolution actually built without having
 * to deserialize the binary env file.
 *
 * "Dominant" is computed by greedy NEAT speciation against a threshold —
 * same algorithm as the in-game Genetic Clusters viewer. We pick the
 * largest cluster, choose its representative (the first protozoan that
 * seeded that cluster), and dump:
 *   - cluster population & % of total
 *   - signatures (plant key, receiving, phagocytic)
 *   - top-level vitals (radius, generation, health, energy)
 *   - GRN topology: input / output / hidden neuron counts
 *   - every synapse (in label, out label, weight, disabled flag)
 *
 * Plain text rather than JSON so the file is grep-able and the synapse
 * table reads like a debug dump rather than a data blob.
 */
public final class DominantLineageReport {

    /** Same threshold as GeneticClustersScreen — keep them in sync. */
    private static final float SPECIATION_THRESHOLD = 1.5f;
    /** Cap the synapse table to avoid 10k-line files on big genomes. */
    private static final int MAX_SYNAPSES_LISTED = 500;

    private DominantLineageReport() {}

    public static void write(Environment env, String path) throws IOException {
        if (env == null) return;

        // Cluster both populations independently. Plant ecology and
        // protozoa ecology are separate axes of selection, so we want a
        // dominant for each.
        List<Cell> protozoa = collectCells(env, Protozoan.class);
        List<Cell> plants   = collectCells(env, PlantCell.class);
        List<Cluster> protoClusters = cluster(protozoa);
        List<Cluster> plantClusters = cluster(plants);

        Cluster dominantProto = largest(protoClusters);
        Cluster dominantPlant = largest(plantClusters);

        try (FileWriter w = new FileWriter(path)) {
            w.write("Dominant Lineage Report\n");
            w.write("=======================\n");
            w.write(String.format("Generated:           sim_time=%.1fs%n", env.getElapsedTime()));
            w.write("\n");

            w.write("===== PROTOZOA =====\n\n");
            if (dominantProto == null)
                w.write("No living protozoa at save time.\n\n");
            else
                writeClusterReport(w, env, dominantProto,
                        protozoa.size(), protoClusters.size());

            w.write("\n");
            w.write("===== PLANTS =====\n\n");
            if (dominantPlant == null)
                w.write("No living plants at save time.\n");
            else
                writeClusterReport(w, env, dominantPlant,
                        plants.size(), plantClusters.size());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Cell> List<Cell> collectCells(Environment env, Class<T> type) {
        List<Cell> out = new ArrayList<>();
        for (Cell c : new ArrayList<>(env.getCells())) {
            if (type.isInstance(c)) out.add(c);
        }
        return out;
    }

    private static List<Cluster> cluster(List<Cell> cells) {
        List<Cluster> clusters = new ArrayList<>();
        for (Cell c : cells) {
            NetworkGenome g;
            try { g = ((com.protoevo.biology.cells.EvolvableCell) c).getGeneExpressionFunction().getGRNGenome(); }
            catch (Throwable t) { continue; }
            if (g == null) continue;

            Cluster best = null;
            float bestDist = Float.POSITIVE_INFINITY;
            for (Cluster cl : clusters) {
                float d;
                try { d = g.distance(cl.representativeGenome); }
                catch (Throwable t) { continue; }
                if (Float.isNaN(d)) continue;
                if (d < bestDist) { bestDist = d; best = cl; }
            }
            if (best != null && bestDist < SPECIATION_THRESHOLD) {
                best.members.add(c);
            } else {
                Cluster fresh = new Cluster(c, g);
                fresh.members.add(c);
                clusters.add(fresh);
            }
        }
        return clusters;
    }

    private static Cluster largest(List<Cluster> cs) {
        if (cs.isEmpty()) return null;
        Cluster d = cs.get(0);
        for (Cluster c : cs) if (c.members.size() > d.members.size()) d = c;
        return d;
    }

    private static void writeClusterReport(FileWriter w, Environment env, Cluster dominant,
                                           int totalCells, int nClusters) throws IOException {
        Cell rep = dominant.representative;
        NetworkGenome g = dominant.representativeGenome;
        w.write(String.format("Total cells:         %d%n", totalCells));
        w.write(String.format("Distinct clusters:   %d (NEAT distance threshold %.2f)%n",
                nClusters, SPECIATION_THRESHOLD));
        w.write(String.format("Dominant cluster:    %d cells (%.1f%% of population)%n%n",
                dominant.members.size(),
                100.0 * dominant.members.size() / totalCells));

        w.write("Representative cell\n");
        w.write("-------------------\n");
        w.write(String.format("  Generation:          %d%n", rep.getGeneration()));
        w.write(String.format("  Radius:              %.4f%n", rep.getRadius()));
        w.write(String.format("  Health:              %.3f%n", rep.getHealth()));
        w.write(String.format("  Energy available:    %.2f J%n", rep.getEnergyAvailable()));
        w.write(String.format("  Time alive:          %.1fs%n", rep.getTimeAlive()));
        w.write(String.format("  Mutation count (NN): %d%n", g.getMutationCount()));
        w.write("\n");

        w.write("Surface signatures\n");
        w.write("------------------\n");
        if (rep instanceof Protozoan) {
            Protozoan p = (Protozoan) rep;
            writeSig(w, "Plant Receptor Key      (50)", p.getPlantReceptorKey());
            writeSig(w, "Receiving Receptor      (75)", p.getProtozoaReceivingReceptor());
            writeSig(w, "Phagocytic Receptor     (75)", p.getProtozoaPhagocyticReceptor());
        } else if (rep instanceof PlantCell) {
            writeSig(w, "Plant Surface Signature (50)",
                    ((PlantCell) rep).getSurfaceSignature());
        }
        w.write("\n");

        w.write("Evolvable trait values\n");
        w.write("----------------------\n");
        dumpTraits(w, ((com.protoevo.biology.cells.EvolvableCell) rep).getGeneExpressionFunction());
        w.write("\n");

        w.write("Gene-regulatory network\n");
        w.write("-----------------------\n");
        int nSensors = 0, nOutputs = 0, nHidden = 0;
        Iterator<NeuronGene> it = g.iterateNeuronGenes();
        while (it.hasNext()) {
            NeuronGene n = it.next();
            if (n == null) continue;
            switch (n.getType()) {
                case SENSOR: nSensors++; break;
                case OUTPUT: nOutputs++; break;
                case HIDDEN: nHidden++; break;
            }
        }
        int totalSyn = g.getSynapseGenes() == null ? 0 : g.getSynapseGenes().length;
        int activeSyn = 0;
        if (g.getSynapseGenes() != null) {
            for (SynapseGene s : g.getSynapseGenes())
                if (!s.isDisabled()) activeSyn++;
        }
        w.write(String.format("  Sensors:  %d%n", nSensors));
        w.write(String.format("  Outputs:  %d%n", nOutputs));
        w.write(String.format("  Hidden:   %d%n", nHidden));
        w.write(String.format("  Synapses: %d total, %d active, %d disabled%n%n",
                totalSyn, activeSyn, totalSyn - activeSyn));

        w.write("Synapses\n");
        w.write("--------\n");
        w.write(String.format("  %-32s -> %-32s  %10s  %s%n",
                "in", "out", "weight", "status"));
        if (g.getSynapseGenes() != null) {
            int shown = 0;
            for (SynapseGene s : g.getSynapseGenes()) {
                if (shown >= MAX_SYNAPSES_LISTED) {
                    w.write(String.format("  ... %d more synapses not shown%n",
                            g.getSynapseGenes().length - shown));
                    break;
                }
                String inLabel  = s.getIn()  == null ? "?" : nullToDash(s.getIn().getLabel());
                String outLabel = s.getOut() == null ? "?" : nullToDash(s.getOut().getLabel());
                w.write(String.format("  %-32s -> %-32s  %+10.4f  %s%n",
                        inLabel, outLabel, s.getWeight(),
                        s.isDisabled() ? "DISABLED" : "active"));
                shown++;
            }
        }
    }

    private static void writeSig(FileWriter w, String label, AminoAcidSequence s) throws IOException {
        w.write(String.format("  %s: %s%n", label, s == null ? "(none)" : s.toString()));
    }

    private static void dumpTraits(FileWriter w, GeneExpressionFunction fn) throws IOException {
        if (fn == null) { w.write("  (no GeneExpressionFunction)\n"); return; }
        try {
            for (String name : fn.getTraitNames()) {
                try {
                    Object v = fn.getGeneValue(name);
                    String pretty = v == null ? "null"
                            : (v instanceof Float
                                ? String.format("%.4f", (Float) v)
                                : v.toString());
                    w.write(String.format("  %-32s = %s%n", name, pretty));
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            w.write("  (trait enumeration failed: " + t.getMessage() + ")\n");
        }
    }

    private static String nullToDash(String s) { return s == null ? "-" : s; }

    private static final class Cluster {
        final Cell representative;
        final NetworkGenome representativeGenome;
        final List<Cell> members = new ArrayList<>();
        Cluster(Cell rep, NetworkGenome g) {
            this.representative = rep;
            this.representativeGenome = g;
        }
    }
}
