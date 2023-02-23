package com.protoevo.test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.MoleculeFunctionalContext;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.env.JointsManager;
import com.protoevo.utils.FileIO;
import com.protoevo.utils.file.ParticleKeyDeserializer;

import java.io.IOException;
import java.util.ArrayList;

public class TestSaveLoad {

    public static class TestEnv {
        public ArrayList<Cell> cells = new ArrayList<>();
    }

    public static void main(String[] args) {
//        Simulation simulation = new Simulation(0, "erebus-hitmonchan-modi");
//        System.out.println("loaded");

        Cell protozoan1 = new Protozoan();
        Cell protozoan2 = new Protozoan();
        Cell protozoan3 = new Protozoan();
        JointsManager.Joining joining1 = new JointsManager.Joining(protozoan1, protozoan2);
        protozoan1.registerJoining(joining1);
        protozoan2.registerJoining(joining1);

        TestEnv env = new TestEnv();
        env.cells.add(protozoan2);
        env.cells.add(protozoan1);

        SmileMapper mapper = SmileMapper.builder()
				.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
                .disable(MapperFeature.AUTO_DETECT_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_SETTERS)
                .enable(MapperFeature.AUTO_DETECT_FIELDS)
                .build();

        mapper.setVisibility(
                mapper.getSerializationConfig()
                        .getDefaultVisibilityChecker()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        );
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addKeyDeserializer(ComplexMolecule.class, new ParticleKeyDeserializer());
        simpleModule.addKeyDeserializer(Cell.class, new ParticleKeyDeserializer());
        simpleModule.addKeyDeserializer(MoleculeFunctionalContext.MoleculeFunction.class, new ParticleKeyDeserializer());

        mapper.registerModule(simpleModule);

		PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().build();
		mapper.activateDefaultTyping(ptv); // default to using DefaultTyping.OBJECT_AND_NON_CONCRETE
		mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

//        try {
//            byte[] smileData = mapper.writeValueAsBytes(env);
//            TestEnv otherValue = mapper.readValue(smileData, TestEnv.class);
//            System.out.println(otherValue);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }


        ObjectMapper jsonMapper = FileIO.getJsonMapper();

        try {
            String envJson = jsonMapper.writeValueAsString(env);

            System.out.println(envJson);

            TestEnv envReloaded = jsonMapper.readValue(envJson, TestEnv.class);

            System.out.println(envReloaded);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
