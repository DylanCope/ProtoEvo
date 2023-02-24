package com.protoevo.test;

import com.badlogic.gdx.utils.Json;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.protoevo.core.Statistics;
import com.protoevo.utils.FileIO;

public class TestStats {

    public static void main(String[] args) {
        Statistics statistics = new Statistics();
        statistics.putCount("Test Count", 16);
        statistics.putPercentage("Test Percentage", 60f);
        statistics.putTime("Test Time", 60f);
        statistics.putDistance("Test Distance", 5f);
        statistics.putMass("Test Mass", 5f);
        statistics.putEnergy("Test Energy", 5f);
        statistics.putSpeed("Test Speed", 5000f);
        statistics.putForce("Test Force", 5000f);
        statistics.putAcceleration("Test Acceleration", -50000f);
        statistics.putAcceleration("Test Acceleration2", -5000000f);
        statistics.putArea("Test Area", 1.23e-9f);
        statistics.putArea("Test Area2", -1.23e-6f);
        statistics.put("Test Float", -1.23e6f);
        statistics.putBoolean("Test Boolean", true);

        statistics.forEach(System.out::println);

        ObjectMapper mapper = FileIO.getJsonMapper();
        try {
            System.out.println(mapper.writeValueAsString(statistics));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
