package com.protoevo.networking;

import com.protoevo.core.Simulation;
import com.protoevo.env.EnvFileIO;
import com.protoevo.env.Environment;

public class RemoteGraphics {

    private final Client client;
    private final Simulation simulation;

    public RemoteGraphics(String remoteAddress, Simulation simulation, int port) {
        client = new Client(remoteAddress, port);
        this.simulation = simulation;
    }

    public RemoteGraphics(String remoteAddress, Simulation simulation) {
        this(remoteAddress, simulation, 8888);
    }

    public void send() {
        client.send(simulation.getEnv(), Environment.class);
    }

    public void close() {
        client.close();
    }

    public Client.Status getStatus() {
        return client.getStatus();
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation("test");
        RemoteGraphics remoteGraphics = new RemoteGraphics("127.0.0.1", simulation, 1212);
        simulation.prepare();
        System.out.println(simulation.getEnv().getStats());
        remoteGraphics.send();
        System.out.println(remoteGraphics.getStatus());
        remoteGraphics.close();
        simulation.close();
    }
}
