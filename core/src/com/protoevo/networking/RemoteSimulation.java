package com.protoevo.networking;

import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.settings.SimulationSettings;

import java.util.Optional;

public class RemoteSimulation extends Simulation {
    private final Server environmentServer;

    public RemoteSimulation() {
        environmentServer = new Server(8888);
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

    @Override
    public void prepare() {
        environment = getEnvironmentFromServer();
        initialised = true;
        if (manager != null) {
            manager.notifySimulationReady();
        }
    }

    public void listenForEnvironment() {
        while (!isFinished()) {
            environment = getEnvironmentFromServer();
        }
    }

    private Environment getEnvironmentFromServer() {
        Environment env = environmentServer.get()
                .filter(o -> o instanceof Environment)
                .map(o -> (Environment) o)
                .orElseThrow(() -> new RuntimeException("Could not get environment from server"));
        env.createTransientObjects();
        return env;
    }

    @Override
    public void update() {

    }

    @Override
    public String getLoadingStatus() {
        if (getEnv() == null)
            return environmentServer.getStatus().getMessage();
        if (initialised)
            return "Received environment from server";
        return "Environment received, preparing simulation";
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
        throw new UnsupportedOperationException("Cannot load environment on remote simulation");
    }

    @Override
    public Environment loadMostRecentEnv() {
        throw new UnsupportedOperationException("Cannot load environment on remote simulation");
    }

    @Override
    public boolean isFinished() {
        return environment != null && super.isFinished();
    }

    @Override
    public void requestSave() {

    }

    @Override
    public void printStats() {
        super.printStats();
    }

    @Override
    public void handleCrash(Exception e) {

    }

    @Override
    public void saveOnOtherThread() {

    }

    @Override
    public void interruptSimulationLoop() {
        super.interruptSimulationLoop();
    }

    @Override
    public void close() {

    }

    @Override
    public void dispose() {
        super.dispose();
        environmentServer.close();
    }

    @Override
    public String save() {
        return null;
    }

    @Override
    public void makeStatisticsSnapshot() {

    }

    @Override
    public void togglePause() {

    }

    @Override
    public void setPaused(boolean paused) {

    }

    @Override
    public void setTimeDilation(float td) {

    }


    @Override
    public String getSaveFolder() {
        return null;
    }

    @Override
    public void openSaveFolderOnDesktop() {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void toggleTimeDilation() {

    }
}
