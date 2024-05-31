package com.protoevo.env.serialization;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.ConstructionProject;
import com.protoevo.biology.Food;
import com.protoevo.biology.cells.*;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nn.*;
import com.protoevo.biology.nodes.*;
import com.protoevo.biology.organelles.MoleculeProductionOrganelle;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Statistics;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.env.Rock;
import com.protoevo.physics.Joining;
import com.protoevo.physics.JointsManager;
import com.protoevo.physics.SpatialHash;
import com.protoevo.physics.box2d.Box2DCollisionHandler;
import com.protoevo.utils.Colour;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KryoSerialization {

    public static Kryo getKryo() {
        Kryo kryo = new Kryo();
        kryo.register(Environment.class);
        kryo.register(Cell.class);
        kryo.register(MultiCellStructure.class);
        kryo.register(Food.class);
        kryo.register(CauseOfDeath.class);
        kryo.register(ConstructionProject.class);
        kryo.register(SpatialHash.class);
        kryo.register(Rock.class);
        kryo.register(Box2DCollisionHandler.class);
        kryo.register(Colour.class);
        kryo.register(Protozoan.class);
        kryo.register(SurfaceNode.class);
        kryo.register(NodeAttachment.class);
        kryo.register(Flagellum.class);
        kryo.register(Spike.class);
        kryo.register(PhagocyticReceptor.class);
        kryo.register(Photoreceptor.class);
        kryo.register(AdhesionReceptor.class);
        kryo.register(Organelle.class);
        kryo.register(MoleculeProductionOrganelle.class);
        kryo.register(PlantCell.class);
        kryo.register(MeatCell.class);
        kryo.register(ChemicalSolution.class);
        kryo.register(ComplexMolecule.class);
        kryo.register(Statistics.class);
        kryo.register(Statistics.Stat.class);
        kryo.register(JointsManager.class);
        kryo.register(Joining.class);
        kryo.register(GeneExpressionFunction.class);
        kryo.register(GeneExpressionFunction.ExpressionNode.class);
        kryo.register(GeneExpressionFunction.RegulationNode.class);
        kryo.register(NeuralNetwork.class);
        kryo.register(Neuron.class);
        kryo.register(NetworkGenome.class);
        kryo.register(NeuronGene.class);
        kryo.register(SynapseGene.class);
        kryo.register(Evolvable.class);
        kryo.register(Evolvable.Component.class);
        kryo.register(Evolvable.Element.class);
        kryo.register(Trait.class);
        kryo.register(FloatTrait.class);
        kryo.register(ControlTrait.class);
        kryo.register(BooleanTrait.class);
        kryo.register(IntegerTrait.class);
        kryo.register(CollectionTrait.class);
        kryo.register(Vector2.class);

        kryo.register(HashMap.class);
        kryo.register(HashSet.class);
        kryo.register(Class.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(ConcurrentLinkedQueue.class);
        kryo.register(LinkedHashSet.class);
        kryo.register(ArrayList.class);
        kryo.register(LinkedHashSet.class);
        kryo.register(TreeMap.class);

        return kryo;
    }

    public static byte[] toBytes(Object object, Class<?> clazz) {
        throw new RuntimeException("Kryo serialization not implemented");
    }

    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        throw new RuntimeException("Kryo serialization not implemented");
    }

    public static void serialize(Object object, String filename) {
        Kryo kryo = getKryo();
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             Output out = new Output(outputStream)) {
            kryo.writeClassAndObject(out, object);
        } catch(IOException i) {
            i.printStackTrace();
            throw new RuntimeException(i);
        }
    }

    public static <T> T deserialize(String filename, Class<T> clazz) {
        Kryo kryo = getKryo();
        try (FileInputStream fileIn = new FileInputStream(filename);
             Input input= new Input(fileIn)) {
            return (T) kryo.readClassAndObject(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
