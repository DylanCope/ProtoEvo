package com.protoevo.core.repl;

import com.protoevo.core.ApplicationManager;
import com.protoevo.core.Simulation;

public class Exit extends Command {

    public Exit(REPL repl) {
        super(repl);
    }

    @Override
    public boolean run(String[] args) {
        System.out.println("Exit triggered...");

        Simulation sim = repl.getSimulation();
        if (sim != null)
            sim.close();

        ApplicationManager manager = repl.getManager();
        if (manager != null) {
            manager.exit();
        }
        System.exit(0);
        return false;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"exit", "quit"};
    }

    @Override
    public String getDescription() {
        return "Save and close the simulation and exit the program.";
    }
}
