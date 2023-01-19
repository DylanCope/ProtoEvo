package com.protoevo.core;

import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.protoevo.ui.SimulationScreen;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class REPL implements Runnable
{
    private final Simulation simulation;
    private final SimulationScreen screen;
    private boolean running = true;

    public REPL(Simulation simulation, SimulationScreen screen)
    {
        this.simulation = simulation;
        this.screen = screen;
    }

    public void setTimeDilation(String[] args) throws Exception
    {
        if (args.length != 2)
            throw new Exception("This command takes 2 arguments.");

        float d = Float.parseFloat(args[1]);
        simulation.setTimeDilation(d);
    }

    @Override
    public void run() {
        System.out.println("Starting REPL...");
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        while (running)
        {
            String line;
            try
            {
                System.out.print("> ");
                line = bufferRead.readLine();
                String[] args = line.split(" ");
                String cmd = args[0];
                switch (cmd)
                {
                    case "help":
                        System.out.println("commands - help, quit, stats, settime, gettime, toggleui, togglepause");
                        break;
                    case "quit":
                        synchronized (simulation) {
                            simulation.close();
                        }
//                        Application.exit();
                        break;
                    case "stats":
                        synchronized (simulation) {
                            simulation.printStats();
                        }
                        break;
                    case "settime":
                        setTimeDilation(args);
                        break;
                    case "gettime":
                        synchronized (simulation) {
                            System.out.println(simulation.getTimeDilation());
                        }
                        break;
                    case "toggleui":
                        if (screen == null) {
                            System.out.println("No UI to toggle.");
                        } else {
                            System.out.println("Toggling UI.");
//                        window.getFrame().setVisible(!window.getFrame().isVisible());
                            synchronized (simulation) {
                                simulation.toggleUpdateDelay();
                                screen.toggleEnvironmentRendering();
                            }
                        }
                        break;
                    case "togglepause":
                        simulation.togglePause();
                        System.out.println("Toggling pause.");
                        break;
                    default:
                        System.out.println("Command not recognised.");
                        break;
                }
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}