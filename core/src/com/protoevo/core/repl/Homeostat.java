package com.protoevo.core.repl;

import com.protoevo.core.Simulation;

public class Homeostat extends Command {

    public Homeostat(REPL repl) {
        super(repl);
    }

    @Override
    public boolean run(String[] args) {
        Simulation sim = repl.getSimulation();
        if (args.length == 1) {
            System.out.printf("Homeostat: %s, protozoa target=%d, plant target=%d%n",
                    sim.isHomeostasisEnabled() ? "ON" : "OFF",
                    sim.getHomeostasisTargetPop(),
                    sim.getHomeostasisTargetPlants());
            return true;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "on":
                sim.setHomeostasisEnabled(true);
                return true;
            case "off":
                sim.setHomeostasisEnabled(false);
                return true;
            case "target":
                if (args.length < 3) {
                    System.out.println("Usage: homeostat target <int>");
                    return false;
                }
                try {
                    sim.setHomeostasisTargetPop(Integer.parseInt(args[2]));
                    return true;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid integer: " + args[2]);
                    return false;
                }
            case "plants":
                if (args.length < 3) {
                    System.out.println("Usage: homeostat plants <int>");
                    return false;
                }
                try {
                    sim.setHomeostasisTargetPlants(Integer.parseInt(args[2]));
                    return true;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid integer: " + args[2]);
                    return false;
                }
            default:
                printUsage();
                return false;
        }
    }

    @Override
    public String[] getAliases() {
        return new String[]{"homeostat", "homeo"};
    }

    @Override
    public String getDescription() {
        return "Inspect or control the population homeostat (PID).";
    }

    @Override
    public void printUsage() {
        System.out.println("Usage:");
        System.out.println("  homeostat                — show status");
        System.out.println("  homeostat on|off         — toggle controller");
        System.out.println("  homeostat target <int>   — set protozoa target population");
        System.out.println("  homeostat plants <int>   — set plant target population");
    }
}
