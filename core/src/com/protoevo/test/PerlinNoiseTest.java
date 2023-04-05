package com.protoevo.test;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.utils.Perlin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PerlinNoiseTest {

    public static void drawNoise() {
        // Create a new BufferedImage with white background
        int width = 128;
        int height = 128;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // Save the image as a PNG

        float[][] noise = Perlin.generatePerlinNoise(width, height, 0);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float light = MathUtils.clamp(1 + 2 * noise[x][y], 0, 1);
                g.setColor(new Color(light, light, light));
                g.fillRect(x, y, 1, 1);
            }
        }

        try {
            File file = new File("noise.png");
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        drawNoise();
    }
}
