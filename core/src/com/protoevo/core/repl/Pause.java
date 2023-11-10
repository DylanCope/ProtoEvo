package com.protoevo.core.repl;

public class Pause extends Command {

    public Pause(REPL repl) {
        super(repl);
    }

    @Override
    public boolean run(String[] args) {
        repl.getSimulation().togglePause();
        return true;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"pause", "unpause", "togglepause"};
    }

    @Override
    public String getDescription() {
        return "Pause and unpause the simulation.";
    }
}
