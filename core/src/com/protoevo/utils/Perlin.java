package com.protoevo.utils;

import com.badlogic.gdx.math.MathUtils;

import java.util.Random;

/**
 * Generates Perlin noise.
 */
public class Perlin {
    public static float[][] generatePerlinNoise(int width, int height, int seed) {

        FastNoiseLite noise = new FastNoiseLite(seed);
//        noise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
//        noise.SetFrequency(3f / width);
//        noise.SetFractalOctaves(6);
//        noise.SetFractalWeightedStrength(8.5f);
//        noise.SetFractalLacunarity(1.3f);
//        noise.SetFractalType(FastNoiseLite.FractalType.Ridged);
//        noise.SetFractalGain(0.5f);
        noise.SetFrequency(3f / width);
        noise.SetFractalOctaves(5);
        noise.SetFractalWeightedStrength(0f);
        noise.SetFractalLacunarity(2f);
        noise.SetFractalType(FastNoiseLite.FractalType.FBm);
        noise.SetFractalGain(0.5f);

        noise.SetCellularDistanceFunction(FastNoiseLite.CellularDistanceFunction.EuclideanSq);
        noise.SetCellularJitter(1f);

        float[][] noiseMap = new float[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                noiseMap[x][y] = noise.GetNoise(x, y) * 0.5f;
            }
        }

        return noiseMap;
    }
}
