package com.protoevo.core.repl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class Command {

    protected REPL repl;

    public Command(REPL repl) {
        this.repl = repl;
    }

    public abstract boolean run(String[] args);
    public abstract String[] getAliases();
    public abstract String getDescription();

    public void printDetailedHelp() {
        String[] aliases = getAliases();
        System.out.println("Command: " + aliases[0]);
        System.out.println("Description: " + getDescription());
        if (aliases.length > 1) {
            System.out.print("Aliases: ");
            for (int i = 1; i < aliases.length; i++) {
                System.out.print(aliases[i] + (i < aliases.length - 1 ? ", " : ""));
            }
            System.out.print("\n");
        }
        printUsage();
    }

    public void printUsage() {

    }
}
