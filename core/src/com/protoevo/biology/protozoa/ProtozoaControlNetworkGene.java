package com.protoevo.biology.protozoa;

import com.protoevo.biology.evolution.Gene;
import com.protoevo.biology.neat.NetworkGenome;
import com.protoevo.core.settings.Settings;

import java.io.Serializable;
import java.util.Map;

public class ProtozoaControlNetworkGene implements Gene<NetworkGenome>, Serializable {
    public static final long serialVersionUID = -1259753801126730417L;

    private final NetworkGenome networkGenome;
    private final String geneName;

    public ProtozoaControlNetworkGene(String geneName) {
        this.geneName = geneName;
        networkGenome = newRandomValue();
    }

    public ProtozoaControlNetworkGene(String geneName, NetworkGenome networkGenome) {
        this.geneName = geneName;
        this.networkGenome = networkGenome;
    }

    @Override
    public Gene<NetworkGenome> mutate() {
        NetworkGenome mutated = new NetworkGenome(networkGenome);
        mutated.mutate();
        return createNew(mutated);
    }

    @Override
    public NetworkGenome getValue(Map<String, Object> dependencies) {
        int retinaSize = 0;
        if (dependencies != null && dependencies.containsKey("Retina Size"))
            retinaSize = (int) dependencies.get("Retina Size");

        networkGenome.ensureRetinaSensorsExist(retinaSize);
        return networkGenome;
    }

    @Override
    public NetworkGenome newRandomValue() {
        NetworkGenome networkGenome = new NetworkGenome();
        networkGenome.addFullyConnectedOutput("Turn Amount");
        networkGenome.addFullyConnectedOutput("Speed");
        networkGenome.addFullyConnectedOutput("Mate Desire");
        networkGenome.addFullyConnectedOutput("Attack");
        networkGenome.addFullyConnectedOutput("Growth Control");

        networkGenome.addFullyConnectedSensor("Bias");
        networkGenome.addFullyConnectedSensor("Health");
        networkGenome.addFullyConnectedSensor("Size");
        networkGenome.addFullyConnectedSensor("Mass Available");

        if (Settings.enableChemicalField) {
            networkGenome.addFullyConnectedSensor("Pheromone Gradient X");
            networkGenome.addFullyConnectedSensor("Pheromone Gradient Y");
            networkGenome.addFullyConnectedSensor("Pheromone Amount");
        }

        return networkGenome;
    }

    @Override
    public Gene<NetworkGenome> createNew(NetworkGenome value) {
        return new ProtozoaControlNetworkGene(geneName, value);
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
