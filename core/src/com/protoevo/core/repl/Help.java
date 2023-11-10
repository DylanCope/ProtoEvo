package com.protoevo.core.repl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Help extends Command {

    public Help(REPL repl) {
        super(repl);
    }

    @Override
    public boolean run(String[] args) {
        Map<String, Command> commands = repl.getCommands();
        Set<Command> commandSet = new HashSet<>(commands.values());
        for (Command command : commandSet) {
            String[] aliases = command.getAliases();
            System.out.println(aliases[0] + " - " + command.getDescription());
        }
        System.out.println("Enter <command> -help to get more detailed information.");
        return true;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"help", "h", "gethelp", "info"};
    }

    @Override
    public String getDescription() {
        return "Print descriptions of commands.";
    }
}
