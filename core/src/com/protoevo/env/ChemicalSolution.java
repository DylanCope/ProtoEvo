package com.protoevo.env;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.EdibleCell;
import com.protoevo.biology.Food;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.settings.PerformanceSettings;
import com.protoevo.settings.Settings;
import com.protoevo.settings.SimulationSettings;
import com.protoevo.utils.DebugMode;
import com.protoevo.utils.JCudaKernelRunner;
import com.protoevo.utils.Utils;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ChemicalSolution implements Serializable {
    public static final long serialVersionUID = 1L;

    private final Environment environment;
    private final float cellSizeX, cellSizeY;
    private final float xMin, yMin, xMax, yMax;
    private final int chemicalTextureHeight;
    private final int chemicalTextureWidth;
    private transient Pixmap chemicalPixmap, swapPixmap;
    private final byte[] swapBuffer;
    private float timeSinceUpdate = 0;
    private transient JCudaKernelRunner diffusionKernel;
    private Consumer<Pixmap> updateChemicalsTextureCallback;
    private final transient Color tmpColour = new Color();

    public ChemicalSolution(Environment environment, int cells, float mapRadius) {
        this(environment, -mapRadius, mapRadius, -mapRadius, mapRadius, cells);
    }

    public ChemicalSolution(Environment environment,
                            float xMin, float xMax,
                            float yMin, float yMax,
                            int cells) {
        this.environment = environment;

        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;

        if (!PerformanceSettings.useGPU) {
            cells /= 4;
        }

        this.chemicalTextureWidth = cells;
        this.chemicalTextureHeight = cells;

        this.cellSizeX = cells / (xMax - xMin);
        this.cellSizeY = cells / (yMax - yMin);

        chemicalPixmap = new Pixmap(cells, cells, Pixmap.Format.RGBA8888);
        swapPixmap = new Pixmap(cells, cells, Pixmap.Format.RGBA8888);

        swapBuffer = new byte[cells * cells * 4];
        chemicalPixmap.setBlending(Pixmap.Blending.None);
    }

    public void initialise() {
        if (PerformanceSettings.useGPU) {
            // has to be called on the same thread running the simulation
            if (DebugMode.isDebugMode())
                System.out.println("Initialising chemical diffusion CUDA kernel...");
            diffusionKernel = new JCudaKernelRunner("diffusion");
        }
    }

    public float getFieldWidth() {
        return xMax - xMin;
    }

    public float getFieldHeight() {
        return yMax - yMin;
    }

    public Vector2 toEnvironmentCoords(int i, int j) {
        float x = xMin + (0.5f + i) * cellSizeX;
        float y = yMin + (0.5f + j) * cellSizeY;
        return new Vector2(x, y);
    }

    public int toChemicalGridXDist(float dist) {
        return (int) (dist * cellSizeX);
    }

    public int toChemicalGridX(float x) {
        return (int) Utils.linearRemap(x, xMin, xMax, 0, chemicalTextureWidth);
    }

    public boolean inBounds(float x, float y) {
        return x >= xMin && x <= xMax && y >= yMin && y <= yMax;
    }

    private int toFloatBufferIndex(int x, int y) {
        return (y * chemicalTextureWidth+ x) * 4;
    }

    public int toChemicalGridY(float y) {
        return (int) Utils.linearRemap(y, yMin, yMax, 0, chemicalTextureHeight);
    }

    public void depositChemicals(float delta, Cell e) {
        float worldX = e.getPos().x;
        float worldY = -e.getPos().y;

        if (!inBounds(worldX, worldY))
            return;

        if (e instanceof EdibleCell && !e.isDead()) {
            float deposit = Settings.plantPheromoneDeposit * delta;
            Color cellColour = e.getColor();

            int fieldX = toChemicalGridX(worldX);
            int fieldY = toChemicalGridY(worldY);

            chemicalPixmap.setColor(cellColour.r, cellColour.g, cellColour.b, deposit);
            chemicalPixmap.fillCircle(fieldX, fieldY, toChemicalGridXDist(e.getRadius()));
        }
        else if (e instanceof Protozoan) {
            Protozoan protozoan = (Protozoan) e;

            int size = toChemicalGridXDist(e.getRadius());
            int x = toChemicalGridX(worldX);
            int y = toChemicalGridY(worldY);
            for (int i = -size; i <= size; i++) {
                for (int j = -size; j <= size; j++) {
                    if (i*i + j*j <= size*size) {
                        int fieldX = x + i;
                        int fieldY = y + j;

                        if (fieldX >= 0 && fieldX < chemicalTextureWidth &&
                                fieldY >= 0 && fieldY < chemicalTextureHeight) {
                            int colourRGBA8888 = chemicalPixmap.getPixel(fieldX, fieldY);
                            tmpColour.set(colourRGBA8888);
                            float amount = Utils.linearRemap(
                                    i*i + j*j, size*size / 4f, size*size,
                                    1, 0.1f);
                            float extraction = (1 + delta) * amount * tmpColour.a;

                            if (tmpColour.g > 0.75f)
                                protozoan.addFood(Food.Type.Plant, extraction * tmpColour.g * 1e-5f);

                            if (tmpColour.r > 0.75f)
                                protozoan.addFood(Food.Type.Meat, extraction * tmpColour.r * 1e-5f);

                            tmpColour.a = Math.max(0, tmpColour.a - extraction);
                            chemicalPixmap.drawPixel(fieldX, fieldY, tmpColour.toIntBits());
                        }
                    }
                }
            }
        }
    }

    public void deposit(float delta) {
        environment.getCells().parallelStream()
                .forEach(e -> depositChemicals(delta, e));
    }

    private void cudaDiffuse() {
        IntStream.range(0, chemicalTextureWidth * chemicalTextureHeight).parallel()
                .forEach(i -> {
                    int x = i % chemicalTextureWidth;
                    int y = i / chemicalTextureWidth;
                    int colourRGBA8888 = chemicalPixmap.getPixel(x, y);

                    swapBuffer[4*i] = (byte) ((colourRGBA8888 & 0xff000000) >>> 24);
                    swapBuffer[4*i + 1] = (byte) ((colourRGBA8888 & 0x00ff0000) >>> 16);
                    swapBuffer[4*i + 2] = (byte) ((colourRGBA8888 & 0x0000ff00) >>> 8);
                    swapBuffer[4*i + 3] = (byte) ((colourRGBA8888 & 0x000000ff));
                });

        try {
            if (diffusionKernel == null)
                initialise();

            diffusionKernel.processImage(
                    swapBuffer, chemicalTextureWidth, chemicalTextureHeight);
        }
        catch (Exception e) {
            if (e.getMessage().contains("CUDA_ERROR_INVALID_CONTEXT") ||
                    e.getMessage().contains("CUDA_ERROR_INVALID_HANDLE")) {
                if (DebugMode.isDebugMode())
                    System.out.println("CUDA context lost, reinitialising...");
                initialise();
            } else {
                throw e;
            }
        }

        IntStream.range(0, chemicalTextureWidth * chemicalTextureHeight).parallel()
                .forEach(i -> {
                    int x = i % chemicalTextureWidth;
                    int y = i / chemicalTextureWidth;
                    int r = swapBuffer[4*i] & 0xFF;
                    int g = swapBuffer[4*i + 1] & 0xFF;
                    int b = swapBuffer[4*i + 2] & 0xFF;
                    int a = swapBuffer[4*i + 3] & 0xFF;
                    int colour = (r << 24) | (g << 16) | (b << 8) | a;

                    chemicalPixmap.drawPixel(x, y, colour);
                });
    }

    public void cpuDiffuse() {
        IntStream.range(0, chemicalTextureWidth * chemicalTextureHeight).parallel()
                .forEach(idx -> {
                    int x = idx % chemicalTextureWidth;
                    int y = idx / chemicalTextureWidth;

                    // See voidStartDistance in SimulationSettings
                    float world_radius = 30.0f;

                    int width = this.chemicalTextureWidth;
                    int height = this.chemicalTextureHeight;
                    float cellSizeX = 2 * world_radius / ((float) width);
                    float cellSizeY = 2 * world_radius / ((float) height);
                    float world_x = -world_radius + cellSizeX * x;
                    float world_y = -world_radius + cellSizeY * y;
                    float dist2_to_world_centre = world_x*world_x + world_y*world_y;

                    // set alpha decay to zero as we approach the void
                    float decay = 0.0f;

                    float void_p = 0.9f;
                    if (dist2_to_world_centre > void_p * void_p * world_radius * world_radius) {
                        float dist_to_world_centre = (float) Math.sqrt(dist2_to_world_centre);
                        // lerp from 1.0 to 0.0 for distance between void_p*world_radius and world_radius
                        decay = 0.9995f * (1.0f - (dist_to_world_centre - void_p * world_radius) / ((1.0f - void_p) * world_radius));
                        if (decay < 0.0) {
                            decay = 0.0f;
                        }
                    } else {
                        decay = 0.9995f;
                    }
                    int channels = 4;
                    int FILTER_SIZE = 3;
                    int alpha_channel = channels - 1;
                    float final_alpha = 0.0f;
                    int radius = (FILTER_SIZE - 1) / 2;

                    Color color = new Color(), tmpColour = new Color();
                    for (int i = -radius; i <= radius; i++) {
                        for (int j = -radius; j <= radius; j++) {
                            int x_ = x + i, y_ = y + j;
                            if (x_ < 0 || x_ >= width || y_ < 0 || y_ >= height) {
                                continue;
                            }
                            Color.rgba8888ToColor(color, chemicalPixmap.getPixel(x_, y_));
                            final_alpha += color.a;
                        }
                    }
                    final_alpha = decay * final_alpha / ((float) (FILTER_SIZE*FILTER_SIZE));
                    color.a = final_alpha;

                    if (final_alpha < 5.0 / 255.0) {
                        chemicalPixmap.setColor(0);
                        return;
                    }

                    float[] tmp = new float[channels - 1];
                    for (int i = -radius; i <= radius; i++) {
                        for (int j = -radius; j <= radius; j++) {
                            int x_ = x + i, y_ = y + j;
                            if (x_ < 0 || x_ >= width || y_ < 0 || y_ >= height) {
                                continue;
                            }
                            Color.rgba8888ToColor(tmpColour, chemicalPixmap.getPixel(x_, y_));
                            tmp[0] += decay * tmpColour.r * tmpColour.a;
                            tmp[1] += decay * tmpColour.g * tmpColour.a;
                            tmp[2] += decay * tmpColour.b * tmpColour.a;
                        }
                        tmp[0] = tmp[0] / ((float) (FILTER_SIZE*FILTER_SIZE)) * decay / final_alpha;
                        tmp[1] = tmp[1] / ((float) (FILTER_SIZE*FILTER_SIZE)) * decay / final_alpha;
                        tmp[2] = tmp[2] / ((float) (FILTER_SIZE*FILTER_SIZE)) * decay / final_alpha;
                    }
                    color.r = tmp[0];
                    color.g = tmp[1];
                    color.b = tmp[2];

                    swapPixmap.drawPixel(x, y, Color.rgba8888(color));
                });

        Pixmap tmp = chemicalPixmap;
        chemicalPixmap = swapPixmap;
        swapPixmap = tmp;
    }

    public void diffuse() {
        if (PerformanceSettings.useGPU)
            cudaDiffuse();
        else
            cpuDiffuse();
    }

    public void update(float delta) {
        if (chemicalPixmap == null) {
            chemicalPixmap = new Pixmap(chemicalTextureWidth, chemicalTextureHeight, Pixmap.Format.RGBA8888);
            swapPixmap = new Pixmap(chemicalTextureWidth, chemicalTextureHeight, Pixmap.Format.RGBA8888);
        }

        timeSinceUpdate += delta;
        if (timeSinceUpdate > SimulationSettings.chemicalDiffusionInterval) {
            diffuse();
            timeSinceUpdate = 0;
            if (updateChemicalsTextureCallback != null)
                updateChemicalsTextureCallback.accept(chemicalPixmap);
        }
        deposit(delta);
    }

    public Pixmap getChemicalPixmap() {
        return chemicalPixmap;
    }

    public int getNYChunks() {
        return chemicalTextureHeight;
    }

    public int getNXChunks() {
        return chemicalTextureWidth;
    }

    public float getPlantPheromoneDensity(float x, float y) {
        int i = toChemicalGridX(x);
        int j = toChemicalGridY(y);
        return getPlantPheromoneDensity(i, j);
    }

    public float getPlantPheromoneDensity(int i, int j) {
        if (i < 0 || i >= chemicalTextureWidth || j < 0 || j >= chemicalTextureHeight)
            return 0;

        int chemicalColour = chemicalPixmap.getPixel(i, j);
        Color.rgba8888ToColor(tmpColour, chemicalColour);
        return tmpColour.g * tmpColour.a;
    }

    public float getMinX() {
        return xMin;
    }

    public float getMaxX() {
        return xMax;
    }

    public float getMinY() {
        return yMin;
    }

    public float getMaxY() {
        return yMax;
    }

    public void depositCircle(Vector2 pos, float r, Color c) {
        int i = toChemicalGridX(pos.x);
        int j = toChemicalGridY(-pos.y);
        int rc = toChemicalGridXDist(r);
        chemicalPixmap.setColor(c);
        chemicalPixmap.fillCircle(i, j, rc);
    }

    public float getCellSize() {
        return cellSizeX;
    }

    public void setUpdateCallback(Consumer<Pixmap> updateChemicalsTextureCallback) {
        this.updateChemicalsTextureCallback = updateChemicalsTextureCallback;
    }
}
