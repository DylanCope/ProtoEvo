package com.protoevo.utils;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.env.LightManager;
import com.protoevo.env.Rock;
import com.protoevo.ui.rendering.EnvironmentRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EnvironmentImageRenderer {

    private final Environment environment;
    private final int width, height;

    float zoom = 1.2f;
    float worldMinX = zoom * -Environment.settings.worldgen.radius.get();
    float worldMinY = zoom * -Environment.settings.worldgen.radius.get();
    float worldMaxX = zoom * Environment.settings.worldgen.radius.get();
    float worldMaxY = zoom * Environment.settings.worldgen.radius.get();

    public EnvironmentImageRenderer(int w, int h, Environment environment) {
        this.environment = environment;
        width = w;
        height = h;
    }

    public void background(Graphics2D graphics)
    {
        Color backgroundColour = new Color(
                EnvironmentRenderer.backgroundColor.r,
                EnvironmentRenderer.backgroundColor.g,
                EnvironmentRenderer.backgroundColor.b
        );
        graphics.setColor(backgroundColour);

        graphics.fillRect(0, 0, width, height);
        ChemicalSolution chemicalSolution = environment.getChemicalSolution();
        for (float x = chemicalSolution.getMinX(); x < chemicalSolution.getMaxX(); x += chemicalSolution.getCellSize()) {
            int imageX = toImageSpaceX(x);
            int nextImageX =  toImageSpaceX(x + chemicalSolution.getCellSize());
            int cellWidth =  Math.max(1, Math.abs(nextImageX - imageX));
            for (float y = chemicalSolution.getMinY(); y < chemicalSolution.getMaxY(); y+= chemicalSolution.getCellSize()) {
                int imageY = toImageSpaceY(-y);
                int nextImageY =  toImageSpaceY(-y - chemicalSolution.getCellSize());
                Colour colour = chemicalSolution.getColour(x, y);
                int cellHeight =  Math.max(1, Math.abs(nextImageY - imageY));
                graphics.setColor(new Color(colour.r, colour.g, colour.b, 0.5f * colour.a));
                graphics.fillRect(imageX, imageY, cellWidth, cellHeight);
            }
        }
    }

    public int toImageSpaceX(float worldX) {
        return Math.round((worldX - worldMinX) / (worldMaxX - worldMinX) * width);
    }

    public int toImageSpaceY(float worldY) {
        return Math.round((worldY - worldMinY) / (worldMaxY - worldMinY) * height);
    }

    public int toImageDistance(float dist) {
        return toImageSpaceX(dist) - toImageSpaceX(0);
    }

    public void renderRocks(Graphics2D g) {
        for (Rock rock : environment.getRocks()) {
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];
            for (int i = 0; i < 3; i++) {
                xPoints[i] = toImageSpaceX(rock.getPoints()[i].x);
                yPoints[i] = toImageSpaceY(rock.getPoints()[i].y);
            }
            Colour colour = rock.getColour();
            Color color = new Color(colour.r, colour.g, colour.b, colour.a);
            g.setColor(color);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.fillPolygon(xPoints, yPoints, 3);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color.darker());
            Stroke s = g.getStroke();
            float strokeR = 0.02f * Environment.settings.worldgen.maxRockSize.get();
            g.setStroke(new BasicStroke(Math.max(1, toImageDistance(strokeR))));
//            g.setStroke(new BasicStroke(1));
            for (int i = 0; i < rock.getEdges().length; i++) {
                if (rock.isEdgeAttached(i))
                    continue;
                Vector2[] edge = rock.getEdge(i);
                int startX = toImageSpaceX(edge[0].x);
                int startY = toImageSpaceY(edge[0].y);
                int endX = toImageSpaceX(edge[1].x);
                int endY = toImageSpaceY(edge[1].y);

                g.drawLine(startX, startY, endX, endY);
            }
            g.setStroke(s);
        }
    }

    public void drawCells(Graphics2D graphics) {
        for (Cell cell : environment.getCells()) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color cellColor = new Color(cell.getColor().r, cell.getColor().g, cell.getColor().b);
            graphics.setColor(cellColor);
            int imageX = toImageSpaceX(cell.getPos().x);
            int imageY = toImageSpaceY(cell.getPos().y);
            int imageR = toImageDistance(cell.getRadius());
            graphics.fillOval(imageX - imageR, imageY - imageR, 2*imageR, 2*imageR);
            graphics.setStroke(new BasicStroke(1));
            graphics.setColor(cellColor.darker());
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawOval(imageX - imageR, imageY - imageR, 2*imageR, 2*imageR);
        }
    }

    public void renderImage(String outputDir) {
        // Create a new BufferedImage with white background
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        background(g);
        drawCells(g);
        renderRocks(g);

        // Save the image as a PNG
        try {
            File file = new File(outputDir + "/world.png");
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void drawLightMap(String outputDir, LightManager lightManager) {
        // Create a new BufferedImage with white background
        int width = lightManager.getWidth();
        int height = lightManager.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // Save the image as a PNG

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float light = lightManager.getCellLight(x, y);
                g.setColor(new Color(light, light, light));
                g.fillRect(x, y, 1, 1);
            }
        }

        try {
            File file = new File(outputDir + "/lightmap.png");
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void render(String outputDir) {
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        renderImage(outputDir);
//        drawLightMap(outputDir, environment.getLightMap());
    }
}
