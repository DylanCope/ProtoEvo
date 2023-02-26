package com.protoevo.test;

import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Simulation;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class ApplicationTest {

    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    public static void main(String[] args) throws IOException {
        deleteDir(new File("saves/test"));
        Simulation sim = new Simulation("test");
        sim.prepare();
        sim.update();
        sim.close();

        Desktop.getDesktop().open(new File(sim.getSaveFolder()));

        String name = sim.getName();
        Simulation reloaded = new Simulation(name);

        Collection<Cell> reloadedCells = reloaded.getEnv().getCells();
        assert reloadedCells.size() == sim.getEnv().getCells().size();
    }
}
