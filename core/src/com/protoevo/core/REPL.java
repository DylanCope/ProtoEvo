package com.protoevo.core;

import com.protoevo.env.Environment;
import com.protoevo.networking.RemoteGraphics;
import com.protoevo.settings.Settings;
import com.protoevo.utils.EnvironmentImageRenderer;

import java.io.BufferedReader;
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

    private final Map<String, Function<String[], Boolean>> commands = new HashMap<>();

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
        commands.put("setparam", this::setParam);
        commands.put("set", this::setParam);
        commands.put("getparam", this::getParam);
        commands.put("get", this::getParam);
        commands.put("screenshot", this::screenshot);
        commands.put("s", this::screenshot);
        commands.put("setremotegraphics", this::setRemoteGraphics);
        commands.put("setrg", this::setRemoteGraphics);
        commands.put("sendremotegraphics", this::sendGraphics);
        commands.put("sendrg", this::sendGraphics);
    }

    public Boolean help(String[] args) {
        System.out.println("Available commands:");
        System.out.println("help - Display this help message.");
        System.out.println("toggleui - Toggle the UI.");
        System.out.println("exit - Exit the program.");
        System.out.println("settime <float> - Set the time dilation.");
        System.out.println("gettime - Get the time dilation.");
        System.out.println("stats - Print simulation statistics.");
        System.out.println("pause - Pause the simulation.");
        System.out.println("setparam <param> <value> - Set a parameter.");
        System.out.println("setparam -help - Get help on setting parameters.");
        System.out.println("getparam <param> - Get a parameter.");
        System.out.println("screenshot - Take a screenshot.");
        return true;
    }

    public Boolean screenshot(String[] args) {
        try {
            EnvironmentImageRenderer renderer = new EnvironmentImageRenderer(
                    1024, 1024, simulation.getEnv()
            );
            String outputDir = simulation.getSaveFolder() + "/screenshots";
            renderer.render(outputDir);
            System.out.println("Created images in directory: " + outputDir);
        } catch (Exception e) {
            System.out.println("Failed to create screenshot: " + e);
            return false;
        }
        return true;
    }

    public Boolean setRemoteGraphics(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("This command takes 1 or 2 arguments.");
            return false;
        }
        if (args[1] != null && !args[1].equals("")) {
            manager.setRemoteGraphics(new RemoteGraphics(args[1], simulation));
            System.out.println("Remote graphics set to " + args[1]);
        } else {
            System.out.println("Invalid argument: " + args[1]);
            return false;
        }
        return true;
    }

    public Boolean sendGraphics(String[] args) {
        if (args.length != 1) {
            System.out.println("This command takes no arguments.");
            return false;
        }

        if (!manager.hasRemoteGraphics()) {
            System.out.println("Remote graphics not set. Use setremotegraphics <address> to set.");
            return false;
        }

        System.out.println("Sending remote graphics...");
        manager.sendRemoteGraphics();
        return true;
    }

    public Boolean setParam(String[] args) {
        if (args.length == 2 && args[1].equals("-help")) {
            System.out.println("This command takes 2 arguments.");
            System.out.println("Usage: setparam <param> <value>");
            System.out.println("Example: setparam protozoa.starvationRate 100");
            System.out.println("Possible subsetting categories: world, protozoa, plant, misc");
            System.out.println("Use setparam <subcategory> -help to get more information about a parameter.");
            System.out.println("Available base parameters:");
            for (Settings.Parameter<?> param : Environment.settings.getParameters())
                System.out.println("\t- " + param.getFieldName() + ": "
                        + param.getName() + "; " + param.getDescription());
            return true;
        }
        if (args.length != 3) {
            System.out.println("This command takes 2 arguments.");
            return false;
        }
        try {
            String paramName = args[1];
            String subcategory = "base";

            if (args[2].equals("-help"))
                subcategory = paramName;
            else if (args[1].contains(".")) {
                String[] split = args[1].split("\\.");
                subcategory = split[0];
                paramName = split[1];
            }

            Settings settings = Environment.settings.getSettings(subcategory);

            if (args[2].equals("-help")) {
                System.out.println("Available parameters in " + subcategory + " category:");
                for (Settings.Parameter<?> param : settings.getParameters())
                    System.out.println("\t- " + param.getFieldName() + ": "
                            + param.getName() + "; " + param.getDescription());
                return true;
            }

            for (Settings.Parameter<?> param : settings.getParameters()) {
                if (param.getFieldName().equals(paramName)) {
                    try {
                        param.set(args[2]);
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

    public Boolean getParam(String[] args) {
        if (args.length > 2) {
            System.out.println("This command takes 1 argument.");
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

            Settings settings = Environment.settings.getSettings(subcategory);

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

    public Boolean pause(String[] args) {
        simulation.togglePause();
        return true;
    }

    public Boolean getTimeDilation(String[] args) {
        System.out.println("Time dilation is " + simulation.getTimeDilation());
        return true;
    }

    public Boolean setTimeDilation(String[] args) {
        if (args.length != 2) {
            System.out.println("This command takes 2 arguments.");
            return false;
        }
        try {
            float d = Float.parseFloat(args[1]);
            simulation.setTimeDilation(d);
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Invalid argument.");
        }
        return false;
    }

    public Boolean printStats(String[] args) {
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

    public Boolean exit(String[] args) {
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

    public Boolean toggleUI(String[] args) {
        if (manager.isOnlyHeadless()) {
            System.out.println("Cannot toggle UI when only headless.");
            return false;
        }

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