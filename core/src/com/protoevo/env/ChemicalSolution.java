package com.protoevo.env;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.EdibleCell;
import com.protoevo.biology.PlantCell;
import com.protoevo.core.settings.Settings;
import com.protoevo.core.settings.SimulationSettings;
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
    private final Pixmap chemicalPixmap;
    private final byte[] swapBuffer;
    private float timeSinceUpdate = 0;
    private JCudaKernelRunner diffusionKernel;
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

        this.chemicalTextureWidth = cells;
        this.chemicalTextureHeight = cells;

        this.cellSizeX = cells / (xMax - xMin);
        this.cellSizeY = cells / (yMax - yMin);

        chemicalPixmap = new Pixmap(cells, cells, Pixmap.Format.RGBA8888);

        swapBuffer = new byte[cells * cells * 4];
        chemicalPixmap.setBlending(Pixmap.Blending.None);
    }

    public void initialise() {
        // has to be called on the same thread running the simulation
        System.out.println("Initialising chemical diffusion CUDA kernel...");
        diffusionKernel = new JCudaKernelRunner("diffusion");
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

    private int toFloatBufferIndex(int x, int y) {
        return (y * chemicalTextureWidth+ x) * 4;
    }

    public int toChemicalGridY(float y) {
        return (int) Utils.linearRemap(y, yMin, yMax, 0, chemicalTextureHeight);
    }

    public void depositChemicals(float delta, Cell e) {
        if (e instanceof EdibleCell && !e.isDead()) {
            float k = Settings.plantPheromoneDeposit;
            float deposit = 500f * delta;
            Color cellColour = e.getColor();

            int fieldX = toChemicalGridX(e.getPos().x);
            int fieldY = toChemicalGridY(-e.getPos().y);

            chemicalPixmap.setColor(cellColour.r, cellColour.g, cellColour.b, deposit);
            chemicalPixmap.fillCircle(fieldX, fieldY, toChemicalGridXDist(e.getRadius()));
        }
    }

    public void deposit() {
        environment.getCells().parallelStream()
                .forEach(e -> depositChemicals(timeSinceUpdate, e));
    }

    public void diffuse() {
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
            diffusionKernel.processImage(
                    swapBuffer, chemicalTextureWidth, chemicalTextureHeight);
        }
        catch (Exception e) {
            if (e.getMessage().contains("CUDA_ERROR_INVALID_CONTEXT") ||
                    e.getMessage().contains("CUDA_ERROR_INVALID_HANDLE")) {
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

    public void update(float delta) {
        timeSinceUpdate += delta;
        deposit();
        if (timeSinceUpdate > SimulationSettings.chemicalDiffusionInterval) {
            diffuse();
            timeSinceUpdate = 0;
            if (updateChemicalsTextureCallback != null)
                updateChemicalsTextureCallback.accept(chemicalPixmap);
        }
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
