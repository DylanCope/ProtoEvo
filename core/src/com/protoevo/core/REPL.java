package com.protoevo.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class REPL implements Runnable
{
    private final Simulation simulation;
    private volatile boolean running = true;
    private BufferedReader bufferRead;
    private InputStreamReader input;
    private ApplicationManager manager;

    private final Map<String, Function<Object[], Boolean>> commands = new HashMap<>();

    public REPL(Simulation simulation)
    {
        this.simulation = simulation;

        commands.put("toggleui", this::toggleUI);
        commands.put("ui", this::toggleUI);
        commands.put("help", this::help);
        commands.put("h", this::help);
        commands.put("exit", this::exit);
        commands.put("quit", this::exit);
        commands.put("settime", this::setTimeDilation);
        commands.put("gettime", this::getTimeDilation);
        commands.put("stats", this::printStats);
        commands.put("getstats", this::printStats);
        commands.put("pause", this::pause);
        commands.put("togglepause", this::pause);
        commands.put("unpause", this::pause);
//        commands.put("setparam", this::setParam);
    }

    public Boolean help(Object[] args) {
        System.out.println("Available commands:");
        System.out.println("help - Display this help message.");
        System.out.println("toggleui - Toggle the UI.");
        System.out.println("exit - Exit the program.");
        System.out.println("settime <float> - Set the time dilation.");
        System.out.println("gettime - Get the time dilation.");
        System.out.println("stats - Print simulation statistics.");
        System.out.println("pause - Pause the simulation.");
//        System.out.println("setparam <param> <value> - Set a parameter. Available parameters are:");
//        for (String param : Settings.paramsMap.keySet()) {
//            System.out.println("\t- " + param);
//        }
        return true;
    }

//    public Boolean setParam(Object[] args) {
//        if (args.length != 3) {
//            System.out.println("This command takes 2 arguments.");
//            return false;
//        }
//        try {
//            String param = (String) args[1];
//            float value = Float.parseFloat((String) args[2]);
//            if (!Settings.paramsMap.containsKey(param)) {
//                System.out.println("Invalid parameter.");
//                return false;
//            }
//            Settings.paramsMap.get(param).apply(value);
//            System.out.println("Set " + param + " to " + value);
//            return true;
//        } catch (NumberFormatException e) {
//            System.out.println("Invalid argument.");
//        }
//        return false;
//    }

    public Boolean pause(Object[] args) {
        simulation.togglePause();
        return true;
    }

    public Boolean getTimeDilation(Object[] args) {
        System.out.println("Time dilation is " + simulation.getTimeDilation());
        return true;
    }

    public Boolean setTimeDilation(Object[] args) {
        if (args.length != 2) {
            System.out.println("This command takes 2 arguments.");
            return false;
        }
        try {
            float d = Float.parseFloat((String) args[1]);
            simulation.setTimeDilation(d);
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Invalid argument.");
        }
        return false;
    }

    public Boolean printStats(Object[] args) {
        simulation.printStats();
        if (args.length == 2 && args[1].equals("--debug")) {
            simulation.getEnv().getDebugStats().forEach(
                    stat -> System.out.println(stat.toString())
            );
            simulation.getEnv().getPhysicsDebugStats().forEach(
                    stat -> System.out.println(stat.toString())
            );
        }
        return true;
    }

    public Boolean exit(Object[] args) {
        System.out.println("Exit triggered...");
        if (manager != null) {
            running = false;
            manager.exit();
        } else {
            simulation.close();
            System.exit(0);
        }
        return false;
    }

    public Boolean toggleUI(Object[] args) {
        if (manager != null) {
            System.out.println("Toggling UI.");
            synchronized (simulation) {
                manager.toggleGraphics();
            }
            return true;
        } else {
            System.out.println("No UI to toggle.");
        }
        return false;
    }

    public void close() {
        System.out.println("\nClosing REPL...");
        running = false;
        try {
            input.close();
            bufferRead.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String stripWhitespace(String s) {
        return s.replaceAll("\\s+", "");
    }

    @Override
    public void run() {
        System.out.println("Starting REPL...\nType 'help' for a list of commands.");
        input = new InputStreamReader(System.in);
        bufferRead = new BufferedReader(input);
        while (running)
        {
            String line;
            try
            {
                System.out.print("> ");
                line = bufferRead.readLine();

                if (line.equals("\n") || stripWhitespace(line).equals("")) {
                    System.out.println();
                    continue;
                }

                String[] args = line.split(" ");
                String cmd = args[0];
                if (commands.containsKey(cmd)) {
                    commands.get(cmd).apply(args);
                } else {
                    System.out.println("Command not recognised.");
                }
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void setManager(ApplicationManager manager) {
        this.manager = manager;
    }
}