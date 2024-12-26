package com.protoevo.test;

import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Simulation;
import com.protoevo.env.serialization.KryoSerialization;
import com.protoevo.env.serialization.Serialization;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

public class SerializationTest {

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

    public record TestResults(
        Duration preparationTime,
        Duration saveTime,
        Duration reloadingTime
    ) {}

    private static TestResults runSerializationTest(Serialization.Backend backend) throws IOException {

        Serialization.SERIALIZATION_BACKEND = backend;
        System.out.println("Running Test with serialization backend: " + backend.name());

        deleteDir(new File("saves/serialization_test"));

        Instant start = Instant.now();
        Simulation sim = new Simulation("serialization_test");
        sim.prepare();
        Instant end = Instant.now();
        Duration preparationTime = Duration.between(start, end);
        System.out.println("\nPreparation time: " + preparationTime.toMillis() + "ms");

        sim.update();

        start = Instant.now();
        sim.close();
        end = Instant.now();
        Duration saveTime = Duration.between(start, end);
        System.out.println("\nSave time: " + saveTime.toMillis() + "ms");

        Desktop.getDesktop().open(new File(sim.getSaveFolder()));

        String name = sim.getName();
        start = Instant.now();
        Simulation reloaded = new Simulation(name);
        reloaded.prepare();
        end = Instant.now();
        Duration reloadingTime = Duration.between(start, end);
        System.out.println("\nReloading time: " + reloadingTime.toMillis() + "ms");

        Collection<Cell> reloadedCells = reloaded.getEnv().getCells();
        assert reloadedCells.size() == sim.getEnv().getCells().size();

        sim.close();

        return new TestResults(preparationTime, saveTime, reloadingTime);
    }

    public static void main(String[] args) throws IOException {

        KryoSerialization.WARN_UNREGISTERED_CLASSES = true;
        TestResults kryoResults = runSerializationTest(Serialization.Backend.KRYO);
    }
}
