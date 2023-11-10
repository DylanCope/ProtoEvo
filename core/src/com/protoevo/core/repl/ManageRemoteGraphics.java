package com.protoevo.core.repl;

import com.protoevo.core.ApplicationManager;
import com.protoevo.core.Simulation;
import com.protoevo.networking.RemoteGraphics;

public class ManageRemoteGraphics extends Command {

    public ManageRemoteGraphics(REPL repl) {
        super(repl);
    }

    public boolean setRemoteGraphics(String[] args) {
        if (args.length == 3) {
            System.out.println("This command takes 2 arguments.");
            return false;
        }
        String addr = args[2];
        if (addr != null && !addr.equals("")) {
            ApplicationManager manager = repl.getManager();
            Simulation simulation = repl.getSimulation();
            manager.setRemoteGraphics(new RemoteGraphics(addr, simulation));
            System.out.println("Remote graphics set to " + addr);
        } else {
            System.out.println("Invalid argument: " + addr);
            return false;
        }
        return true;
    }

    public boolean sendGraphics(String[] args) {
        if (args.length != 2) {
            System.out.println("This command takes no additional arguments.");
            return false;
        }

        ApplicationManager manager = repl.getManager();

        if (!manager.hasRemoteGraphics()) {
            System.out.println("Remote graphics not set. Use remote set <address> to set.");
            return false;
        }

        System.out.println("Sending remote graphics...");
        manager.sendRemoteGraphics();
        return true;
    }

    @Override
    public void printUsage() {
        System.out.println("Usage: remote [set|send] <address> <port>");
    }

    @Override
    public boolean run(String[] args) {
        if (args.length < 2) {
            System.out.println("This command takes at least 1 argument.");
            printUsage();
            return false;
        }
        if (args[1].equals("set"))
            return setRemoteGraphics(args);
        else if (args[1].equals("send"))
            return sendGraphics(args);
        else
            System.out.println("Invalid argument 1.");
        return false;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"remote", "remotegraphics", "rg"};
    }

    @Override
    public String getDescription() {
        return "Manage remote graphics.";
    }
}
