package com.protoevo.networking;

import com.protoevo.core.Simulation;
import com.protoevo.env.Environment;
import com.protoevo.settings.SimulationSettings;

import java.util.Optional;

public class RemoteSimulation extends Simulation {
    private final Server environmentServer;
    private String loadingStatus;

    public RemoteSimulation() {
        environmentServer = new Server(8888);
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

//    @Override
//    public void prepare() {
//        getEnvironmentFromServer();
//        initialised = true;
//        if (manager != null) {
//            manager.notifySimulationReady();
//        }
//    }

    private Environment getEnvironmentFromServer() {
        Environment downloadedEnv = environmentServer.get(Environment.class)
                .orElseThrow(() -> new RuntimeException("Could not get environment from server"));
        loadingStatus = environmentServer.getStatus().getMessage();
        downloadedEnv.createTransientObjects();
        downloadedEnv.rebuildWorld();
        initialised = true;
        setName(downloadedEnv.getSimulationName());
        newSaveDir(getName());
        loadingStatus = "Saving local copy of the environment";
        save();
        loadingStatus = "Ready to simulate";
        return downloadedEnv;
    }

    @Override
    public void update() {
        if (hasLoadedEnv()) {
            super.update();
        }
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

//    @Override
//    public void requestSave() {
//
//    }
//
//    @Override
//    public void printStats() {
//        super.printStats();
//    }
//
//    @Override
//    public void handleCrash(Exception e) {
//
//    }
//
//    @Override
//    public void saveOnOtherThread() {
//
//    }
//
//    @Override
//    public void interruptSimulationLoop() {
//        super.interruptSimulationLoop();
//    }
//
//    @Override
//    public void close() {
//
//    }

    @Override
    public void dispose() {
        super.dispose();
        environmentServer.close();
    }

//    @Override
//    public String save() {
//        return null;
//    }
//
//    @Override
//    public void makeStatisticsSnapshot() {
//
//    }
//
//    @Override
//    public void togglePause() {
//
//    }
//
//    @Override
//    public void setPaused(boolean paused) {
//
//    }
//
//    @Override
//    public void setTimeDilation(float td) {
//
//    }
//
//
//    @Override
//    public String getSaveFolder() {
//        return null;
//    }
//
//    @Override
//    public void openSaveFolderOnDesktop() {
//
//    }
//
//    @Override
//    public void toggleTimeDilation() {
//
//    }
}
