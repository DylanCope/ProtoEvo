package com.protoevo.networking;

import com.protoevo.core.Simulation;
import com.protoevo.env.Serialization;
import com.protoevo.env.Environment;
import com.protoevo.settings.SimulationSettings;

public class RemoteSimulation extends Simulation {
    private final Server environmentServer;
    private String loadingStatus;

    public RemoteSimulation() {
        this(8888);
    }

    public RemoteSimulation(int port) {
        environmentServer = new Server(port);
        environmentLoader = this::getEnvironmentFromServer;
    }

    public RemoteSimulation(long seed) {
        throw new UnsupportedOperationException("Cannot create new environment on remote simulation");
    }

    public RemoteSimulation(String name) {
        throw new UnsupportedOperationException("Cannot create new environment on remote simulation");
    }

    public RemoteSimulation(String name, SimulationSettings settings) {
        throw new UnsupportedOperationException("Cannot create new environment on remote simulation");
    }

    public RemoteSimulation(long seed, String name) {
        throw new UnsupportedOperationException("Cannot create new environment on remote simulation");
    }

    public RemoteSimulation(String name, String save) {
        throw new UnsupportedOperationException("Cannot create new environment on remote simulation");
    }

    public RemoteSimulation(long seed, String name, String save) {
        throw new UnsupportedOperationException("Cannot create new environment on remote simulation");
    }

    private Environment getEnvironmentFromServer() {
        byte[] downloadedEnvBytes = environmentServer.get(byte[].class)
                .orElseThrow(() -> new RuntimeException("Could not get environment from server"));
        Environment downloadedEnv = Serialization.fromBytes(downloadedEnvBytes, Environment.class);
        loadingStatus = environmentServer.getStatus().getMessage();
        downloadedEnv.createTransientObjects();
        downloadedEnv.rebuildWorld();
        initialised = true;
        setName(downloadedEnv.getSimulationName());
        if (getName() != null) {
            newSaveDir(getName());
            loadingStatus = "Saving local copy of the environment";
            save();
        }
        loadingStatus = "Ready to simulate";
        return downloadedEnv;
    }

    public void clearLoadedEnvironment() {
        if (environment != null)
            environment.dispose();
        environment = null;
        loadingStatus = "Waiting to load environment";
        initialised = false;
    }

    public boolean hasLoadedEnv() {
        return environment != null;
    }

    @Override
    public String getLoadingStatus() {
        if (getEnv() == null) {
            return environmentServer.getStatus().getMessage();
        }
        return loadingStatus;
    }

    @Override
    public Environment newDefaultEnv() {
        throw new UnsupportedOperationException("Cannot create new environment on remote simulation");
    }

    @Override
    public Environment newEnvironment(SimulationSettings settings) {
        throw new UnsupportedOperationException("Cannot create new environment on remote simulation");
    }

    @Override
    public Environment loadEnv(String filename) {
        if (!hasLoadedEnv())
            throw new UnsupportedOperationException("Have not yet forked environment from remote");
        return super.loadEnv(filename);
    }

    @Override
    public Environment loadMostRecentEnv() {
        if (!hasLoadedEnv())
            throw new UnsupportedOperationException("Have not yet forked environment from remote");
        return super.loadMostRecentEnv();
    }

    @Override
    public boolean isFinished() {
        return hasLoadedEnv() && super.isFinished();
    }

    @Override
    public void dispose() {
        super.dispose();
        environmentServer.close();
    }

    public static void main(String[] args) {
        RemoteSimulation remoteSimulation = new RemoteSimulation(1212);
        Environment env = remoteSimulation.getEnvironmentFromServer();
        System.out.println(env.getStats());
    }
}
