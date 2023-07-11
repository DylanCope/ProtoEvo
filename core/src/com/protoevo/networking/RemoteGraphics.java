package com.protoevo.networking;

import com.protoevo.core.Simulation;

public class RemoteGraphics {

    private final Client client;
    private final Simulation simulation;

    public RemoteGraphics(String remoteAddress, Simulation simulation) {
        client = new Client(remoteAddress, 8888);
        this.simulation = simulation;
    }

    public void send() {
        client.send(simulation.getEnv());
    }

    public void close() {
        client.close();
    }

    public Client.Status getStatus() {
        return client.getStatus();
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation("test");
        RemoteGraphics remoteGraphics = new RemoteGraphics("127.0.0.1", simulation);
        simulation.prepare();
        remoteGraphics.send();
        remoteGraphics.close();
        simulation.close();
    }
}
