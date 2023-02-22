package com.protoevo.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileIO 
{

	public static void save(Object object, String filename)

	{
//		try (FileOutputStream fileOut = new FileOutputStream(filename + ".dat");
//			 ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
//			out.writeObject(object);
//		} catch (IOException i) {
//			i.printStackTrace();
//		}
		String filePath = filename + ".json";
		File file = new File(filePath);
		ObjectMapper mapper = JsonMapper.builder()
				.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
				.disable(MapperFeature.AUTO_DETECT_GETTERS)
				.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
				.enable(MapperFeature.AUTO_DETECT_FIELDS)
				.build();

		mapper.setVisibility(
				mapper.getSerializationConfig()
						.getDefaultVisibilityChecker()
						.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
						.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
		);
		try {
			mapper.writeValue(file, object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Object load(String filename) throws IOException, ClassNotFoundException {
		FileInputStream fileIn = new FileInputStream(filename + ".dat");
		ObjectInputStream in = new ObjectInputStream(fileIn);
		Object result = in.readObject();
		in.close();
		fileIn.close();
		return result;
	}

	public static void appendLine(String filePath, String line) {
		try {
			Files.write(Paths.get(filePath),
					(line + "\n").getBytes(),
					StandardOpenOption.APPEND);
		} catch (IOException ignored) {}
	}

}
