package com.protoevo.networking;

import com.protoevo.core.Simulation;
import com.protoevo.env.Serialization;
import com.protoevo.env.Environment;

import java.io.IOException;

public class RemoteGraphics {

    private Client client;
    private final Simulation simulation;

    public RemoteGraphics(String remoteAddress, Simulation simulation, int port) {
        client = new Client(remoteAddress, port);
        this.simulation = simulation;
    }

    public RemoteGraphics(String remoteAddress, Simulation simulation) {
        this(remoteAddress, simulation, 8888);
    }

    public void rebuildClient() {
        client.close();
        client = new Client(client.getAddress(), client.getPort());
    }

    public void send() {
        byte[] envBytes = Serialization.toBytes(simulation.getEnv(), Environment.class);
        client.send(envBytes);
    }

    public void close() {
        client.close();
    }

    public Client.Status getStatus() {
        return client.getStatus();
    }

    public static void main(String[] args) throws IOException {
        Simulation simulation = new Simulation("test");
        RemoteGraphics remoteGraphics = new RemoteGraphics("127.0.0.1", simulation, 1212);
        simulation.prepare();
        simulation.update();
        System.out.println(simulation.getEnv().getStats());
        remoteGraphics.send();
        System.out.println(remoteGraphics.getStatus());

        remoteGraphics.close();
        System.exit(0);
    }
}
