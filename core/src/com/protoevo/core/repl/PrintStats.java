package com.protoevo.core.repl;

import com.protoevo.core.Simulation;

public class PrintStats extends Command {

    public PrintStats(REPL repl) {
        super(repl);
    }

    @Override
    public boolean run(String[] args) {
        Simulation simulation = repl.getSimulation();
        if (args.length == 1)
            simulation.printStats();
        else if (args.length == 2) {
            if (args[1].equals("debug")) {
                simulation.getEnv().getDebugStats().forEach(
                        stat -> System.out.println(stat.toString())
                );
                simulation.getEnv().getPhysicsDebugStats().forEach(
                        stat -> System.out.println(stat.toString())
                );
            }
            else if (args[1].equals("protozoa")) {
                simulation.getEnv().getProtozoaSummaryStats().forEach(
                        stat -> System.out.println(stat.toString())
                );
            }
        }
        return true;
    }

    @Override
    public void printUsage() {
        System.out.println("Usage: stats");
        System.out.println("Usage: stats [debug|protozoa]");
    }

    @Override
    public String[] getAliases() {
        return new String[]{"stats"};
    }

    @Override
    public String getDescription() {
        return "Print statistics about the simulation.";
    }
}
