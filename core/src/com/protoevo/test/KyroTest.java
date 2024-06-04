package com.protoevo.test;

import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Simulation;
import com.protoevo.env.serialization.Serialization;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class KyroTest {

    private static void deleteDir(File file) {
        if (!file.exists()) return;

        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    public static void main(String[] args) throws IOException {

        Serialization.SERIALIZATION_BACKEND = Serialization.Backend.NATIVE_JAVA;

        deleteDir(new File("saves/test"));

        Simulation sim = new Simulation("test");
        sim.prepare();
        sim.update();
        sim.close();

        Desktop.getDesktop().open(new File(sim.getSaveFolder()));

        String name = sim.getName();
        Simulation reloaded = new Simulation(name);
        reloaded.prepare();

        Collection<Cell> reloadedCells = reloaded.getEnv().getCells();
        assert reloadedCells.size() == sim.getEnv().getCells().size();
    }
}
