package com.protoevo.test;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.env.Environment;
import com.protoevo.env.Rock;
import com.protoevo.env.WorldGeneration;
import com.protoevo.settings.WorldGenerationSettings;
import com.protoevo.utils.Colour;
import org.checkerframework.checker.units.qual.C;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

public class TriangleDrawer {

    public static void drawRocks(List<Rock> rocks) {
        // Create a new BufferedImage with white background
        int width = 800;
        int height = 800;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

//        float rockWorldMinX = Float.MAX_VALUE;
//        float rockWorldMinY = Float.MAX_VALUE;
//        float rockWorldMaxX = Float.MIN_VALUE;
//        float rockWorldMaxY = Float.MIN_VALUE;
//        for (Rock rock : rocks) {
//            for (Vector2 vertex : rock.getPoints()) {
//                rockWorldMinX = Math.min(rockWorldMinX, vertex.x);
//                rockWorldMinY = Math.min(rockWorldMinY, vertex.y);
//                rockWorldMaxX = Math.max(rockWorldMaxX, vertex.x);
//                rockWorldMaxY = Math.max(rockWorldMaxY, vertex.y);
//            }
//        }

//        float worldWidth = rockWorldMaxX - rockWorldMinX;
//        float worldHeight = rockWorldMaxY - rockWorldMinY;
//        float worldRatio = worldWidth / worldHeight;
//        float imageRatio = (float) width / height;
//        if (worldRatio > imageRatio) {
//            float newWorldHeight = worldWidth / imageRatio;
//            rockWorldMinY -= (newWorldHeight - worldHeight) / 2;
//            rockWorldMaxY += (newWorldHeight - worldHeight) / 2;
//        } else {
//            float newWorldWidth = worldHeight * imageRatio;
//            rockWorldMinX -= (newWorldWidth - worldWidth) / 2;
//            rockWorldMaxX += (newWorldWidth - worldWidth) / 2;
//        }

        float rockWorldMinX = -WorldGenerationSettings.environmentRadius;
        float rockWorldMinY = -WorldGenerationSettings.environmentRadius;
        float rockWorldMaxX = WorldGenerationSettings.environmentRadius;
        float rockWorldMaxY = WorldGenerationSettings.environmentRadius;

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

    public static void main(String[] args) {
        drawRocks(WorldGeneration.generate());
    }
}

