package com.protoevo.utils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import com.protoevo.core.Statistics;
import com.protoevo.core.Statistics.Stat;

public class InfluxWriter {
    private InfluxDBClient client;
    private String token = "my-super-secret-auth-token";
    private String bucket = "my-bucket";
    private String org = "protoevo";

    public InfluxWriter() {
        client = InfluxDBClientFactory.create("http://localhost:8086", token.toCharArray());
    }

    public void write(Statistics stats) {

        // Loop through stats map
        Point point = Point
                .measurement("environment")
                .addTag("host", "host1");
        for(Map.Entry<String, Stat> entry : stats.getStatsMap().entrySet()){
            Stat val = entry.getValue();
            if(val.isNumeric()){
                point.addField(entry.getKey(), val.getDouble());
            }
        }

        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        writeApi.writePoint(bucket, org, point);
    }
}