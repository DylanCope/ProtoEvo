package com.protoevo.core.repl;

import com.protoevo.core.Simulation;
import com.protoevo.utils.EnvironmentImageRenderer;

public class Screenshot extends Command {

    public Screenshot(REPL repl) {
        super(repl);
    }

    @Override
    public boolean run(String[] args) {
        try {
            int size = 1024;
            if (args.length > 1) {
                size = Integer.parseInt(args[1]);
            }

            Simulation simulation = repl.getSimulation();

            System.out.println("Creating screenshot of size " + size + "x" + size + "...");
            EnvironmentImageRenderer renderer = new EnvironmentImageRenderer(
                    size, size, simulation.getEnv()
            );
            String outputDir = simulation.getSaveFolder() + "/screenshots";
            renderer.render(outputDir);
            System.out.println("Created images in directory: " + outputDir);
        }
        catch (Exception e) {
            System.out.println("Failed to create screenshot: " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"screenshot", "ss"};
    }

    @Override
    public String getDescription() {
        return "Save a screenshot of the environment.";
    }
}
