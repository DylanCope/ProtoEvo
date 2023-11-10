package com.protoevo.core.repl;

import com.protoevo.core.ApplicationManager;

public class ToggleUI extends Command {
    public ToggleUI(REPL repl) {
        super(repl);
    }
    @Override
    public boolean run(String[] args) {
        ApplicationManager manager = repl.getManager();
        if (manager.isOnlyHeadless()) {
            System.out.println("Cannot toggle UI when only headless.");
            return false;
        }

        System.out.println("Toggling UI.");
        synchronized (repl.getSimulation()) {
            manager.toggleGraphics();
        }
        return true;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"toggleui", "ui"};
    }

    @Override
    public String getDescription() {
        return "Toggle UI if available";
    }
}
