package com.protoevo.core.repl;

import com.protoevo.core.ApplicationManager;
import com.protoevo.core.Simulation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class REPL implements Runnable
{
    private final Simulation simulation;
    private volatile boolean running = true;
    private ApplicationManager manager;

    private final Map<String, Command> commands = new HashMap<>();

    public REPL(Simulation simulation) {
        this.simulation = simulation;

        Command[] commandsList = new Command[]{
                new ToggleUI(this),
                new Help(this),
                new Exit(this),
                new TimeDilation(this),
                new PrintStats(this),
                new Pause(this),
                new SimParams(this),
                new ManageRemoteGraphics(this),
                new Screenshot(this),
        };

        for (Command command : commandsList) {
            for (String alias : command.getAliases()) {
                commands.put(alias, command);
            }
        }
    }

    public String stripWhitespace(String s) {
        return s.replaceAll("\\s+", "");
    }

    @Override
    public void run() {
        System.out.println("Starting REPL...");
        System.out.println("Enter 'help' for a list of commands.");
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader bufferRead = new BufferedReader(input);
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
                    Command command = commands.get(cmd);
                    if (args.length > 1 && args[1].equals("-help") || args[1].equals("-h")) {
                        command.printDetailedHelp();
                        continue;
                    }
                    boolean success = command.run(args);
                    if (!success)
                        System.out.println("Command failed.");
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

    public ApplicationManager getManager() {
        return this.manager;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public Map<String, Command> getCommands() {
        return commands;
    }

    public void close() {
        System.out.println("\nClosing REPL...");
        running = false;
    }
}