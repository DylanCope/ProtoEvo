package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.ConstructionProject;
import com.protoevo.biology.Food;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.MultiCellStructure;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nn.*;
import com.protoevo.biology.nodes.*;
import com.protoevo.biology.organelles.MoleculeProductionOrganelle;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Statistics;
import com.protoevo.physics.Joining;
import com.protoevo.physics.JointsManager;
import com.protoevo.physics.SpatialHash;
import com.protoevo.physics.box2d.Box2DCollisionHandler;
import com.protoevo.utils.Colour;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Serialization {

    public static FSTConfiguration getFSTConfig() {
        FSTConfiguration fstConfig = FSTConfiguration.createDefaultConfiguration();
        fstConfig.registerClass(
                Environment.class,
                Cell.class,
                MultiCellStructure.class,
                Food.class,
                CauseOfDeath.class,
                ConstructionProject.class,
                SpatialHash.class,
                Rock.class,
                Box2DCollisionHandler.class,
                Colour.class,
                Protozoan.class,
                SurfaceNode.class,
                NodeAttachment.class,
                Flagellum.class,
                Spike.class,
                PhagocyticReceptor.class,
                Photoreceptor.class,
                AdhesionReceptor.class,
                Organelle.class,
                MoleculeProductionOrganelle.class,
                PlantCell.class,
                ChemicalSolution.class,
                ComplexMolecule.class,
                Statistics.class,
                Statistics.Stat.class,
                JointsManager.class,
                Joining.class,
                GeneExpressionFunction.class,
                GeneExpressionFunction.ExpressionNode.class,
                GeneExpressionFunction.RegulationNode.class,
                NeuralNetwork.class,
                Neuron.class,
                NetworkGenome.class,
                NeuronGene.class,
                SynapseGene.class,
                Evolvable.class,
                Evolvable.Component.class,
                Evolvable.Element.class,
                Trait.class,
                FloatTrait.class,
                ControlTrait.class,
                BooleanTrait.class,
                IntegerTrait.class,
                CollectionTrait.class,
                Vector2.class
        );
        return fstConfig;
    }

    public static byte[] toBytes(Object object, Class<?> clazz) {
        FSTConfiguration config = getFSTConfig();
        try (FSTObjectOutput out = config.getObjectOutput()) {
            out.writeObject(object, clazz);
            return out.getCopyOfWrittenBuffer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        FSTConfiguration config = getFSTConfig();
        try {
            return (T) config.getObjectInput(bytes).readObject(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T clone(T object, Class<T> clazz) {
        return fromBytes(toBytes(object, clazz), clazz);
    }

    public static void serialize(Object object, Class<?> clazz, String filename) {
        try (FileOutputStream fileOut = new FileOutputStream(filename);
             FSTObjectOutput out = new FSTObjectOutput(fileOut, getFSTConfig())) {
            out.writeObject(object, clazz);
        } catch(IOException i) {
            i.printStackTrace();
        }
    }

    public static <T> T deserialize(String filename, Class<T> clazz) {
        try (FileInputStream fileIn = new FileInputStream(filename);
             FSTObjectInput in = new FSTObjectInput(fileIn, getFSTConfig())) {
            T object = (T) in.readObject(clazz);
            return object;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveEnvironment(Environment env, String filename)
    {
        try {
            Files.createDirectories(Paths.get(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        serialize(env, Environment.class, filename + "/environment.dat");
    }

    public static Environment reloadEnvironment(String filename) {
        Environment env = deserialize(filename + "/environment.dat", Environment.class);
        env.createTransientObjects();
        env.rebuildWorld();
        return env;
    }

    public static void saveCell(Cell cell, String cellName)
    {
        try {
            Files.createDirectories(Paths.get("saved-cells"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        serialize(cell, cell.getClass(), "saved-cells/" + cellName + ".cell");
    }

}
