package com.protoevo.env.serialization;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.protoevo.biology.*;
import com.protoevo.biology.cells.*;
import com.protoevo.biology.evolution.*;
import com.protoevo.biology.nn.*;
import com.protoevo.biology.nodes.*;
import com.protoevo.biology.organelles.MoleculeProductionOrganelle;
import com.protoevo.biology.organelles.Organelle;
import com.protoevo.core.Statistics;
import com.protoevo.env.*;
import com.protoevo.maths.Shape;
import com.protoevo.physics.Collision;
import com.protoevo.physics.Joining;
import com.protoevo.physics.JointsManager;
import com.protoevo.physics.SpatialHash;
import com.protoevo.physics.box2d.Box2DCollisionHandler;
import com.protoevo.physics.box2d.Box2DJointsManager;
import com.protoevo.physics.box2d.Box2DParticle;
import com.protoevo.physics.box2d.Box2DPhysics;
import com.protoevo.settings.*;
import com.protoevo.utils.Colour;
import com.protoevo.utils.SerializableFunction;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KryoSerialization {

    public static boolean WARN_UNREGISTERED_CLASSES = false;

    public static Kryo getKryo() {
        Kryo kryo = new Kryo();
        kryo.register(Environment.class);
        kryo.register(Cell.class);
        kryo.register(MultiCellStructure.class);
        kryo.register(Food.class);
        kryo.register(Food.Type.class);
        kryo.register(CauseOfDeath.class);
        kryo.register(ConstructionProject.class);
        kryo.register(SpatialHash.class);
        kryo.register(Rock.class);
        kryo.register(Colour.class);
        kryo.register(Protozoan.class);
        kryo.register(SurfaceNode.class);
        kryo.register(NodeAttachment.class);
        kryo.register(Flagellum.class);
        kryo.register(Spike.class);
        kryo.register(PhagocyticReceptor.class);
        kryo.register(PlantOnlyPhagocyticReceptor.class);
        kryo.register(MeatOnlyPhagocyticReceptor.class);
        kryo.register(Photoreceptor.class);
        kryo.register(Photoreceptor.ColourSensitivity.class);
        kryo.register(AdhesionReceptor.class);
        kryo.register(Organelle.class);
        kryo.register(MoleculeProductionOrganelle.class);
        kryo.register(PlantCell.class);
        kryo.register(MeatCell.class);
        kryo.register(ChemicalSolution.class);
        kryo.register(ComplexMolecule.class);
        kryo.register(JointsManager.class);
        kryo.register(Joining.class);
        kryo.register(Joining.Type.class);
        kryo.register(Joining.MetaData.class);
        kryo.register(Joining.DistanceMetaData.class);
        kryo.register(Joining.RopeMetaData.class);
        kryo.register(GeneExpressionFunction.class);
        kryo.register(GeneExpressionFunction.ExpressionNode.class);
        kryo.register(GeneExpressionFunction.ExpressionNodes.class);
        kryo.register(GeneExpressionFunction.RegulationNode.class);
        kryo.register(NeuralNetwork.class);
        kryo.register(Neuron.class);
        kryo.register(Neuron.Type.class);
        kryo.register(Neuron[].class);
        kryo.register(SerializableFunction.class);
        kryo.register(ActivationFn.class);
        kryo.register(ActivationFn.SigmoidFn.class);
        kryo.register(ActivationFn.LinearFn.class);
        kryo.register(ActivationFn.TanhFn.class);
        kryo.register(ActivationFn.StepFn.class);
        kryo.register(ActivationFn.ReLuFn.class);
        kryo.register(ActivationFn.SinFn.class);
        kryo.register(ActivationFn.GaussianFn.class);
        kryo.register(ActivationFn.BooleanInputFn.class);
        kryo.register(ActivationFn.OutputActivationFn.class);
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
        kryo.register(Vector2[].class);
        kryo.register(Vector2[][].class);
        kryo.register(Box2DCollisionHandler.class);
        kryo.register(Box2DPhysics.class);
        kryo.register(Box2DParticle.class);
        kryo.register(Box2DJointsManager.class);
        kryo.register(Collision.class);
        kryo.register(Shape.class);
        kryo.register(Shape.Intersection.class);
        kryo.register(Shape.Intersection[].class);
        kryo.register(GRNFactory.GRNTagRegulatorNode.class);
        kryo.register(GRNFactory.GRNTagRegulatorNode.class);
        kryo.register(NeuronGene[].class);
        kryo.register(SynapseGene[].class);
        kryo.register(GeneExpressionFunction.Regulators.class);
        kryo.register(ProtozoaColourTrait.class);
        kryo.register(DamageEvent.class);
        kryo.register(BurstRequest.class);
        kryo.register(BurstRequest.class);
        kryo.register(Colour[].class);
        kryo.register(Colour[][].class);
        kryo.register(LightManager.class);
        kryo.register(TimeManager.class);
        kryo.register(SimulationSettings.class);
        kryo.register(EnvironmentSettings.class);
        kryo.register(EvolutionSettings.class);
        kryo.register(MiscSettings.class);
        kryo.register(PlantSettings.class);
        kryo.register(ProtozoaSettings.class);
        kryo.register(WorldGenerationSettings.class);
        kryo.register(CellSettings.class);
        kryo.register(Parameter.class);
        kryo.register(GRNFactory.ExpressionNodeGRNTag.class);
        kryo.register(GRNFactory.GRNTagRegulatorNode.class);
        kryo.register(SurfaceNode.ConstantMoleculeFunctionalContext.class);
        kryo.register(SurfaceNode.CandidateConstructor.class);
        kryo.register(Protozoan.SpawnChildFn.class);
        kryo.register(Protozoan.SpawnMeatCell.class);
        kryo.register(PlantCell.PlantSplitFn.class);
        kryo.register(Environment.CellDeadPredicate.class);
        kryo.register(Environment.SpawnPlantInClustersFn.class);
        kryo.register(Environment.SpawnProtozoaInClustersFn.class);

        kryo.register(HashMap.class);
        kryo.register(HashSet.class);
        kryo.register(Class.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(ConcurrentLinkedQueue.class);
        kryo.register(LinkedHashSet.class);
        kryo.register(ArrayList.class);
        kryo.register(LinkedHashSet.class);
        kryo.register(TreeMap.class);

        kryo.register(float[].class);
        kryo.register(float[][].class);
        kryo.register(int[].class);
        kryo.register(double[].class);
        kryo.register(boolean[].class);
        kryo.register(byte[].class);
        kryo.register(char[].class);
        kryo.register(short[].class);
        kryo.register(long[].class);
        kryo.register(Object[].class);
        kryo.register(String[].class);
        kryo.register(Float[].class);
        kryo.register(Double[].class);
        kryo.register(Integer[].class);
        kryo.register(Boolean[].class);

        kryo.register(Float.class);
        kryo.register(Double.class);
        kryo.register(Integer.class);
        kryo.register(Boolean.class);
        kryo.register(Byte.class);
        kryo.register(Character.class);
        kryo.register(Short.class);
        kryo.register(Long.class);
        kryo.register(String.class);
        kryo.register(Date.class);

        kryo.register(Statistics.class);
        kryo.register(Statistics.Stat.class);
        kryo.register(Statistics.BaseUnit.class);
        kryo.register(Statistics.StatType.class);
        kryo.register(Statistics.ComplexUnit.class);

        kryo.register(Random.class, new RandomSerializer());

        kryo.setReferences(true);
        kryo.setRegistrationRequired(false);
        kryo.setWarnUnregisteredClasses(WARN_UNREGISTERED_CLASSES);

        return kryo;
    }

    public static class RandomSerializer extends Serializer<Random> {
        @Override
        public void write(Kryo kryo, Output output, Random random) {
            output.writeLong(random.nextLong()); // Write the next long instead of the seed
        }

        @Override
        public Random read(Kryo kryo, Input input, Class<? extends Random> type) {
            long seed = input.readLong(); // Read and set the new seed
            return new Random(seed);
        }
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
