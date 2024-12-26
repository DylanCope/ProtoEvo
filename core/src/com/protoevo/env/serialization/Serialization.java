package com.protoevo.env.serialization;

import com.protoevo.biology.cells.Cell;
import com.protoevo.env.Environment;
import com.protoevo.env.serialization.custom.CustomSerialization;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Serialization {

    public enum Backend {
        FST,
        KRYO,
        NATIVE_JAVA,
        CUSTOM
    }

    public static Backend SERIALIZATION_BACKEND = Backend.KRYO;

    public static byte[] toBytes(Object object, Class<?> clazz) {
        return switch (SERIALIZATION_BACKEND) {
            case FST -> FSTSerialization.toBytes(object, clazz);
            case KRYO -> KryoSerialization.toBytes(object, clazz);
            case NATIVE_JAVA -> NativeJavaSerialization.toBytes(object);
            case CUSTOM -> CustomSerialization.toBytes(object);
            default -> throw new RuntimeException("Unknown serialization backend");
        };
    }

    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        return switch (SERIALIZATION_BACKEND) {
            case FST -> FSTSerialization.fromBytes(bytes, clazz);
            case KRYO -> KryoSerialization.fromBytes(bytes, clazz);
            case NATIVE_JAVA -> NativeJavaSerialization.fromBytes(bytes);
            default -> throw new RuntimeException("Unknown serialization backend");
        };
    }

    public static <T> T clone(T object, Class<T> clazz) {
        return fromBytes(toBytes(object, clazz), clazz);
    }

    public static void serialize(Object object, Class<?> clazz, String filename) {
        switch (SERIALIZATION_BACKEND) {
            case FST -> FSTSerialization.serialize(object, clazz, filename);
            case KRYO -> KryoSerialization.serialize(object, filename);
            case NATIVE_JAVA -> NativeJavaSerialization.serialize(object, filename);
            default -> throw new RuntimeException("Unknown serialization backend");
        }
    }

    public static <T> T deserialize(String filename, Class<T> clazz) {
        switch (SERIALIZATION_BACKEND) {
            case FST -> {
                return FSTSerialization.deserialize(filename, clazz);
            }
            case KRYO -> {
                return KryoSerialization.deserialize(filename, clazz);
            }
            case NATIVE_JAVA -> {
                return NativeJavaSerialization.deserialize(filename);
            }
            default -> throw new RuntimeException("Unknown serialization backend");
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
