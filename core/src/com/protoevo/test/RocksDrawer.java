package com.protoevo.test;

import com.protoevo.env.Environment;
import com.protoevo.env.LightMap;
import com.protoevo.env.Rock;
import com.protoevo.env.WorldGeneration;
import com.protoevo.ui.DefaultBackgroundGenerator;
import com.protoevo.ui.DefaultBackgroundRenderer;
import com.protoevo.utils.Colour;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

public class RocksDrawer {

    public static void drawRocks(List<Rock> rocks) {
        // Create a new BufferedImage with white background
        int width = 800;
        int height = 800;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

        float rockWorldMinX = -Environment.settings.world.radius.get();
        float rockWorldMinY = -Environment.settings.world.radius.get();
        float rockWorldMaxX = Environment.settings.world.radius.get();
        float rockWorldMaxY = Environment.settings.world.radius.get();

        for (Rock rock : rocks) {
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];
            for (int i = 0; i < 3; i++) {
                xPoints[i] = (int) ((rock.getPoints()[i].x - rockWorldMinX) / (rockWorldMaxX - rockWorldMinX) * width);
                yPoints[i] = (int) ((rock.getPoints()[i].y - rockWorldMinY) / (rockWorldMaxY - rockWorldMinY) * height);
            }
            Colour colour = rock.getColour();
            g.setColor(new Color(colour.r, colour.g, colour.b, colour.a));
            g.fillPolygon(xPoints, yPoints, 3);
        }

        // Save the image as a PNG
        try {
            File file = new File("rocks.png");
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void drawLightMap(LightMap lightMap) {
        // Create a new BufferedImage with white background
        int width = lightMap.getWidth();
        int height = lightMap.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // Save the image as a PNG

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float light = lightMap.getLight(x, y);
                g.setColor(new Color(light, light, light));
                g.fillRect(x, y, 1, 1);
            }
        }

        try {
            File file = new File("lightmap.png");
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Environment.settings = DefaultBackgroundGenerator.createBgEnvSettings();
        List<Rock> rocks = WorldGeneration.generate();
        drawRocks(rocks);

        LightMap lightMap = new LightMap(256, 256, Environment.settings.world.radius.get());
        LightMap.bakeRockShadows(lightMap, rocks);
        drawLightMap(lightMap);
    }
}

