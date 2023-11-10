package com.protoevo.core.repl;

import com.protoevo.core.Simulation;
import com.protoevo.settings.Settings;
import com.protoevo.settings.SimulationSettings;

public class SimParams extends Command {
    public SimParams(REPL repl) {
        super(repl);
    }

    @Override
    public boolean run(String[] args) {
        if (args.length < 3) {
            System.out.println("This command takes at least 2 arguments.");
            printUsage();
            return false;
        }
        switch (args[1]) {
            case "set":
                return setParam(args);
            case "get":
                return getParam(args);
            case "list":
                return list(args);
            default:
                System.out.println("Invalid argument 1.");
                break;
        }

        return false;
    }

    public boolean list(String[] args) {
        if (args.length == 2) {
            Simulation simulation = repl.getSimulation();
            SimulationSettings settings = simulation.getEnv().getSettings();
            System.out.println("Available settings subcategories: world|protozoa|plant|misc|cell|env|evo");
            System.out.println("Run param list <subcategory> to get a list of parameters in that subcategory.");
            System.out.println("Available parameters in base category:");
            for (Settings.Parameter<?> param : settings.getParameters())
                System.out.println("\t- " + param.getFieldName() + ": "
                        + param.getName() + "; " + param.getDescription());
        }
        if (args.length == 3) {
            String subcategory = args[2];

            Simulation simulation = repl.getSimulation();
            Settings settings = simulation.getEnv().getSettings().getSettings(subcategory);

            System.out.println("Available parameters in " + subcategory + " category:");
            for (Settings.Parameter<?> param : settings.getParameters())
                System.out.println("\t- " + param.getFieldName() + ": "
                        + param.getName() + "; " + param.getDescription());
        }
        return true;
    }

    public boolean setParam(String[] args) {
        if (args.length != 4) {
            System.out.println("This command takes 3 arguments.");
            return false;
        }
        try {
            String paramName = args[2];
            String paramVal = args[3];
            String subcategory = "base";

            if (paramName.contains(".")) {
                String[] split = paramName.split("\\.");
                subcategory = split[0];
                paramName = split[1];
            }

            Simulation simulation = repl.getSimulation();
            Settings settings = simulation.getEnv().getSettings().getSettings(subcategory);

            for (Settings.Parameter<?> param : settings.getParameters()) {
                if (param.getFieldName().equals(paramName)) {
                    try {
                        param.set(paramVal);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid argument.");
                        return false;
                    }
                    System.out.println("Set " + paramName + " to " + param.get());
                    return true;
                }
            }
            System.out.println("Could not find parameter " + paramName + " in " + subcategory + " category.");
            return false;
        } catch (NumberFormatException e) {
            System.out.println("Invalid argument.");
        }
        return false;
    }

    public boolean getParam(String[] args) {
        if (args.length > 3) {
            System.out.println("This command takes 2 arguments.");
            return false;
        }
        try {
            String paramName = args[1];
            String subcategory = "base";

            if (args[1].contains(".")) {
                String[] split = args[1].split("\\.");
                subcategory = split[0];
                paramName = split[1];
            }

            Simulation simulation = repl.getSimulation();
            Settings settings = simulation.getEnv().getSettings().getSettings(subcategory);

            for (Settings.Parameter<?> param : settings.getParameters()) {
                if (param.getFieldName().equals(paramName)) {
                    System.out.println(paramName + " is " + param.get());
                    return true;
                }
            }

            System.out.println("Could not find parameter " + paramName + " in " + subcategory + " category.");
            return false;
        } catch (NumberFormatException e) {
            System.out.println("Invalid argument.");
        }
        return false;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"param"};
    }

    @Override
    public void printUsage() {
        System.out.println("Usage: param [get|set|list] [param] [value]");
        System.out.println("Example: param set protozoa.starvationRate 100");
        System.out.println("Example: param list protozoa");
        System.out.println("Example: param get protozoa.starvationRate");
        System.out.println("Possible setting subcategories: world, protozoa, plant, misc, cell, env, evo");
        System.out.println("Use param <subcategory> -help to get more information about a parameter.");
        System.out.println("Available base parameters:");
        Simulation simulation = repl.getSimulation();
        for (Settings.Parameter<?> param : simulation.getEnv().getSettings().getParameters())
            System.out.println("\t- " + param.getFieldName() + ": "
                    + param.getName() + "; " + param.getDescription());
    }

    @Override
    public String getDescription() {
        return "Get or set simulation parameters.";
    }
}
