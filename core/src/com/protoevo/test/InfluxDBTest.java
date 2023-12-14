package com.protoevo.test;

import java.time.Instant;
import java.util.List;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;

public class InfluxDBTest {
    public static void main(String[] args) {
        String token = "my-super-secret-auth-token";
        String bucket = "my-bucket";
        String org = "protoevo";

        InfluxDBClient client = InfluxDBClientFactory.create("http://localhost:8086", token.toCharArray());

        Point point = Point
                .measurement("mem")
                .addTag("host", "host1")
                .addField("used_percent", 23.43234543)
                .time(Instant.now(), WritePrecision.NS);

        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        writeApi.writePoint(bucket, org, point);
    }
}