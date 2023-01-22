package com.protoevo.biology.protozoa;

import com.protoevo.biology.evolution.Trait;
import com.protoevo.biology.neat.NetworkGenome;
import com.protoevo.core.settings.Settings;

import java.io.Serializable;
import java.util.Map;

public class ProtozoaControlNetworkTrait implements Trait<NetworkGenome>, Serializable {
    public static final long serialVersionUID = -1259753801126730417L;

    private final NetworkGenome networkGenome;
    private final String geneName;

    public ProtozoaControlNetworkTrait(String geneName) {
        this.geneName = geneName;
        networkGenome = newRandomValue();
    }

    public ProtozoaControlNetworkTrait(String geneName, NetworkGenome networkGenome) {
        this.geneName = geneName;
        this.networkGenome = networkGenome;
    }

    @Override
    public Trait<NetworkGenome> mutate() {
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
    public Trait<NetworkGenome> createNew(NetworkGenome value) {
        return new ProtozoaControlNetworkTrait(geneName, value);
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
