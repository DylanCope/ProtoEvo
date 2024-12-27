package com.protoevo.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileIO 
{

	public static Path getSavesDir() {
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			return Paths.get(System.getenv("APPDATA") + "/ProtoEvo/saves");
		}
		return Paths.get("saves");
	}

	public static Path getSavedCellsDir() {
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			return Paths.get(System.getenv("APPDATA") + "/ProtoEvo/saved-cells");
		}
		return Paths.get("saved-cells");
	}

	public static ObjectMapper getJsonMapper() {
		ObjectMapper mapper = JsonMapper.builder()
				.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
				.disable(MapperFeature.AUTO_DETECT_GETTERS)
				.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
				.disable(MapperFeature.AUTO_DETECT_SETTERS)
				.enable(MapperFeature.AUTO_DETECT_FIELDS)
				.enable(SerializationFeature.INDENT_OUTPUT)
				.build();

		mapper.setVisibility(
				mapper.getSerializationConfig()
						.getDefaultVisibilityChecker()
						.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
						.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
		);


//		PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().build();
//		mapper.activateDefaultTyping(ptv); // default to using DefaultTyping.OBJECT_AND_NON_CONCRETE
//		mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

		return mapper;
	}

	public static void writeJson(Object object, String filename)
	{
		String filePath = filename.endsWith(".json") ? filename : filename + ".json";
		File file = new File(filePath);
		ObjectMapper mapper = getJsonMapper();
		try {
			mapper.writeValue(file, object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T readJson(String filename, Class<T> clazz) throws IOException {
		String filePath = filename.endsWith(".json") ? filename : filename + ".json";
		File file = new File(filePath);
		ObjectMapper mapper = getJsonMapper();
		return mapper.readValue(file, clazz);
	}

	public static JsonNode readJson(String filename) throws IOException {
		String filePath = filename.endsWith(".json") ? filename : filename + ".json";
		File file = new File(filePath);
		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		return mapper.readTree(file);
	}

	public static void serialize(Object object, String filename)
	{
		try (FileOutputStream fileOut =
					new FileOutputStream(filename + ".dat");
			 ObjectOutputStream out = new ObjectOutputStream(fileOut))
		{
			out.writeObject(object);
		} catch(IOException i) {
			i.printStackTrace();
		}
	}
	
	public static Object deserialize(String filename) throws IOException, ClassNotFoundException {
		try (FileInputStream fileIn = new FileInputStream(filename + ".dat");
			 ObjectInputStream in = new ObjectInputStream(fileIn))
		{
			return in.readObject();
		}
	}

	public static void appendLine(String filePath, String line) {
		try {
			Files.write(Paths.get(filePath),
					(line + "\n").getBytes(),
					StandardOpenOption.APPEND);
		} catch (IOException ignored) {}
	}

}
