package com.protoevo.core;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.ConstructionProject;
import com.protoevo.biology.Food;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nn.*;
import com.protoevo.biology.nodes.*;
import com.protoevo.biology.organelles.MoleculeProductionOrganelle;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.env.*;
import com.protoevo.utils.Colour;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.*;

public class EnvFileIO {
    private static final FSTConfiguration singletonConf = FSTConfiguration.createDefaultConfiguration();
    static {
        singletonConf.registerClass(
                Environment.class,
                Particle.class,
                Cell.class,
                Food.class,
                CauseOfDeath.class,
                ConstructionProject.class,
                SpatialHash.class,
                Rock.class,
                CollisionHandler.class,
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
                JointsManager.Joining.class,
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
                RegulatedFloatTrait.class,
                BooleanTrait.class,
                IntegerTrait.class,
                CollectionTrait.class,
                Vector2.class
        );
    }

    public static FSTConfiguration getFSTConfig() {
        return singletonConf;
    }

    public static void saveEnv(Environment environment, String filename)
    {
        try (FileOutputStream fileOut = new FileOutputStream(filename + ".dat")) {
            getFSTConfig().encodeToStream(fileOut, environment);
        } catch(IOException i) {
            i.printStackTrace();
        }
    }

    public static Environment loadEnv(String filename) throws Exception {
        try (FileInputStream fileIn = new FileInputStream(filename + ".dat"))
        {
            return (Environment) getFSTConfig().decodeFromStream(fileIn);
        }
    }

    public static void serialize(Environment object, String filename)
    {
        try (FileOutputStream fileOut =
                     new FileOutputStream(filename + ".dat");
             FSTObjectOutput out = new FSTObjectOutput(fileOut))
        {
            out.writeObject(object, Environment.class);
        } catch(IOException i) {
            i.printStackTrace();
        }
    }

    public static Environment deserialize(String filename) throws Exception {
        try (FileInputStream fileIn = new FileInputStream(filename + ".dat");
             FSTObjectInput in = new FSTObjectInput(fileIn))
        {
            return (Environment) in.readObject(Environment.class);
        }
    }
}
