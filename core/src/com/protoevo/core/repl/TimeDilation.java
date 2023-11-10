package com.protoevo.core.repl;

import com.protoevo.core.Simulation;

public class TimeDilation extends Command {

    public TimeDilation(REPL repl) {
        super(repl);
    }

    public boolean getTimeDilation(String[] args) {
        Simulation simulation = repl.getSimulation();
        System.out.println("Time dilation is " + simulation.getTimeDilation());
        return true;
    }

    public boolean setTimeDilation(String[] args) {
        if (args.length != 3) {
            System.out.println("This command takes 2 arguments.");
            return false;
        }
        try {
            float d = Float.parseFloat(args[2]);
            repl.getSimulation().setTimeDilation(d);
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Invalid argument.");
        }
        return false;
    }

    @Override
    public boolean run(String[] args) {
        if (args.length < 2) {
            System.out.println("This command takes at least 1 argument.");
            printUsage();
            return false;
        }
        if (args[1].equals("get"))
            return getTimeDilation(args);
        else if (args[1].equals("set"))
            return setTimeDilation(args);
        else
            System.out.println("Invalid argument 1.");
        return false;
    }

    @Override
    public void printUsage() {
        System.out.println("Usage: time [get|set] <time dilation>");
    }

    @Override
    public String[] getAliases() {
        return new String[]{"time", "dilation", "timedilation"};
    }

    @Override
    public String getDescription() {
        return "Get and set the time dilation multiplier of the simulation.";
    }
}
