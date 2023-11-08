package com.protoevo.env;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Food;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.utils.*;

import java.io.Serializable;
import java.util.stream.IntStream;

public class ChemicalSolution implements Serializable {
    public static final long serialVersionUID = 1L;


    private Environment environment;
    private float cellSizeX, cellSizeY;
    private float xMin, yMin, xMax, yMax;
    private int chemicalTextureHeight;
    private int chemicalTextureWidth;
    private transient boolean initialised = false;
    private byte[] byteBuffer;
    private Colour[][] colours;
    private float timeSinceUpdate = 0;
    private transient JCudaKernelRunner diffusionKernel;
    private transient GLComputeShaderRunner diffusionShader;

    public interface ChemicalUpdatedCallback {
        void onChemicalUpdated(int i, int j, Colour colour);
    }
    private transient ChemicalUpdatedCallback updateChemicalCallback;

    public ChemicalSolution() {}

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

        initialise();
    }

    public void setUpdateChemicalCallback(ChemicalUpdatedCallback updateChemicalCallback) {
        this.updateChemicalCallback = updateChemicalCallback;
    }

    public void initialise() {
        if (!initialised) {
            byteBuffer = new byte[chemicalTextureWidth * chemicalTextureHeight * 4];
            colours = new Colour[chemicalTextureWidth][chemicalTextureHeight];
            for (int i = 0; i < chemicalTextureWidth; i++) {
                for (int j = 0; j < chemicalTextureHeight; j++) {
                    colours[i][j] = new Colour();
                }
            }
            diffusionShader = new GLComputeShaderRunner("diffusion");

            initialised = true;
        }

        /*
        if (!JCudaKernelRunner.cudaAvailable())
            Environment.settings.misc.useCUDA.set(false);

        if (Environment.settings.misc.useCUDA.get()) {
            // has to be called on the same thread running the simulation
            if (DebugMode.isDebugMode())
                System.out.println("Initialising chemical diffusion CUDA kernel...");
            //diffusionKernel = new JCudaKernelRunner("diffusion");
        }
        */
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
        return (int) Utils.clampedLinearRemap(x, xMin, xMax, 0, chemicalTextureWidth);
    }

    public boolean outOfWorldBounds(float x, float y) {
        return x < xMin || x > xMax || y < yMin || y > yMax;
    }

    public boolean outOfTextureBounds(int x, int y) {
        return x < 0 || x >= chemicalTextureWidth || y < 0 || y >= chemicalTextureHeight;
    }

    private int toFloatBufferIndex(int x, int y) {
        return (y * chemicalTextureWidth+ x) * 4;
    }

    public int toChemicalGridY(float y) {
        return (int) Utils.clampedLinearRemap(y, yMin, yMax, 0, chemicalTextureHeight);
    }

    public void set(int x, int y, Colour colour) {
        if (outOfTextureBounds(x, y))
            return;
        colours[x][y].set(colour);
        if (updateChemicalCallback != null)
            updateChemicalCallback.onChemicalUpdated(x, y, colour);
    }

    public void set(int x, int y, float r, float g, float b, float a) {
        if (outOfTextureBounds(x, y))
            return;
        colours[x][y].set(r, g, b, a);
        if (updateChemicalCallback != null)
            updateChemicalCallback.onChemicalUpdated(x, y, colours[x][y]);
    }

    public void set(int x, int y, int rgba8888) {
        if (outOfTextureBounds(x, y))
            return;
        colours[x][y].set(rgba8888);
        if (updateChemicalCallback != null)
            updateChemicalCallback.onChemicalUpdated(x, y, colours[x][y]);
    }

    public void cellChemicalIO(float delta, Cell e) {
        float worldX = e.getPos().x;
        float worldY = -e.getPos().y;

        if (outOfWorldBounds(worldX, worldY))
            return;

        if (e.isEdible() && !e.isDead()) {
            Colour cellColour = e.getColour();
            depositCircle(
                    e.getPos(), e.getRadius(),
                    cellColour.r, cellColour.g, cellColour.b, 1f);
        }
        else if (e instanceof Protozoan) {
            protozoanIO(delta, (Protozoan) e);
        }
    }

    private void protozoanIO(float delta, Protozoan protozoan) {
        float worldX = protozoan.getPos().x;
        float worldY = -protozoan.getPos().y;

        int size = toChemicalGridXDist(protozoan.getRadius());
        int x = toChemicalGridX(worldX);
        int y = toChemicalGridY(worldY);

        float cellWorldWidth = getFieldWidth() / chemicalTextureWidth;
        float cellWorldHeight = getFieldHeight() / chemicalTextureHeight;

        for (int i = -size; i <= size; i++) {
            for (int j = -size; j <= size; j++) {
                if (i*i + j*j <= size*size) {
                    int fieldX = x + i;
                    int fieldY = y + j;

                    if (fieldX >= 0 && fieldX < chemicalTextureWidth &&
                            fieldY >= 0 && fieldY < chemicalTextureHeight) {
                        Colour colour = colours[fieldX][fieldY];

                        float cellX = xMin + fieldX * cellWorldWidth;
                        float cellY = yMin + fieldY * cellWorldHeight;

                        float overlapArea = Geometry.boxAndCircleIntersectionOverlap(
                                cellX, cellX + cellWorldWidth,  cellY, cellY + cellWorldHeight,
                                worldX, worldY, protozoan.getRadius()
                        );
                        float overlapP = overlapArea / (cellWorldWidth * cellWorldHeight);
                        float extraction =
                                Environment.settings.cell.chemicalExtractionFactor.get() * delta * overlapP;
                        if (extraction > 0) {

                            if (colour.g > 0.5f && colour.g > 1.5f * colour.r && colour.g > 1.5f * colour.b)
                                protozoan.addFood(Food.Type.Plant,
                                        extraction * colour.g * colour.g
                                                * Environment.settings.cell.chemicalExtractionPlantConversion.get());

                            if (colour.r > 0.5f && colour.r > 1.5f * colour.g && colour.r > 1.5f * colour.b)
                                protozoan.addFood(Food.Type.Meat,
                                        extraction * colour.r * colour.r
                                                * Environment.settings.cell.chemicalExtractionMeatConversion.get());

                            colour.sub(extraction);
                            set(fieldX, fieldY, colour);
                        }
                    }
                }
            }
        }
    }

    public void deposit(float delta) {
        environment.getCells().parallelStream()
                .forEach(e -> cellChemicalIO(delta, e));
    }

    private void loadIntoByteBuffer() {
        IntStream.range(0, chemicalTextureWidth * chemicalTextureHeight).parallel()
                .forEach(i -> {
                    int x = i % chemicalTextureWidth;
                    int y = i / chemicalTextureWidth;
                    int colourRGBA8888 = colours[x][y].getRGBA8888();

                    byteBuffer[4*i] = (byte) ((colourRGBA8888 & 0xff000000) >>> 24);
                    byteBuffer[4*i + 1] = (byte) ((colourRGBA8888 & 0x00ff0000) >>> 16);
                    byteBuffer[4*i + 2] = (byte) ((colourRGBA8888 & 0x0000ff00) >>> 8);
                    byteBuffer[4*i + 3] = (byte) ((colourRGBA8888 & 0x000000ff));
                });
    }

    private void unloadFromByteBuffer() {
        IntStream.range(0, chemicalTextureWidth * chemicalTextureHeight).parallel()
                .forEach(i -> {
                    int x = i % chemicalTextureWidth;
                    int y = i / chemicalTextureWidth;
                    int r = byteBuffer[4*i] & 0xFF;
                    int g = byteBuffer[4*i + 1] & 0xFF;
                    int b = byteBuffer[4*i + 2] & 0xFF;
                    int a = byteBuffer[4*i + 3] & 0xFF;
                    int colour = (r << 24) | (g << 16) | (b << 8) | a;

                    set(x, y, colour);
                });
    }

    private void cudaDiffuse() {

        loadIntoByteBuffer();

        try {
            if (diffusionShader == null)
                initialise();

            diffusionShader.processImage(
                    byteBuffer, chemicalTextureWidth, chemicalTextureHeight);
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

        unloadFromByteBuffer();
    }

    private void diffuseAt(int x, int y) {

        // See voidStartDistance in SimulationSettings
        float world_radius = Environment.settings.worldgen.voidStartDistance.get();

        int width = this.chemicalTextureWidth;
        int height = this.chemicalTextureHeight;
        float cellSizeX = 2 * world_radius / ((float) width);
        float cellSizeY = 2 * world_radius / ((float) height);
        float world_x = -world_radius + cellSizeX * x;
        float world_y = -world_radius + cellSizeY * y;
        float dist2_to_world_centre = world_x*world_x + world_y*world_y;

        // set alpha decay to zero as we approach the void
        float decay;

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
        float final_alpha = 0.0f;
        int radius = (FILTER_SIZE - 1) / 2;

        Colour newColour = new Colour();
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                int x_ = x + i, y_ = y + j;
                if (x_ < 0 || x_ >= width || y_ < 0 || y_ >= height) {
                    continue;
                }
                final_alpha += colours[x_][y_].a;
            }
        }
        final_alpha = decay * final_alpha / ((float) (FILTER_SIZE*FILTER_SIZE));
        newColour.a = final_alpha;

        if (final_alpha < 5.0 / 255.0) {
            return;
        }

        float[] tmp = new float[channels - 1];
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                int x_ = x + i, y_ = y + j;
                if (x_ < 0 || x_ >= width || y_ < 0 || y_ >= height) {
                    continue;
                }
                Colour pixel = colours[x_][y_];
                tmp[0] += decay * pixel.r * pixel.a;
                tmp[1] += decay * pixel.g * pixel.a;
                tmp[2] += decay * pixel.b * pixel.a;
            }
            tmp[0] = tmp[0] / ((float) (FILTER_SIZE*FILTER_SIZE)) * decay / final_alpha;
            tmp[1] = tmp[1] / ((float) (FILTER_SIZE*FILTER_SIZE)) * decay / final_alpha;
            tmp[2] = tmp[2] / ((float) (FILTER_SIZE*FILTER_SIZE)) * decay / final_alpha;
        }
        newColour.r = tmp[0];
        newColour.g = tmp[1];
        newColour.b = tmp[2];

        set(x, y, newColour);
    }

    public void cpuDiffuse() {
        for (int sample = 0; sample < Environment.settings.misc.chemicalCPUIterations.get(); sample++) {
            int i = MathUtils.random(chemicalTextureWidth * chemicalTextureHeight);
            int x = i % chemicalTextureWidth;
            int y = i / chemicalTextureWidth;
            diffuseAt(x, y);
        }
    }

    public void diffuse() {
        if (Environment.settings.misc.useCUDA.get())
            cudaDiffuse();
        else
            cpuDiffuse();
    }

    public void update(float delta) {
        if (!initialised) {
            initialise();
        }

        timeSinceUpdate += delta;
        if (timeSinceUpdate > delta * Environment.settings.env.chemicalDiffusionInterval.get()) {
            diffuse();
            timeSinceUpdate = 0;
        }
        deposit(delta);
    }

    public Colour[][] getImage() {
        if (!initialised) {
            initialise();
        }

        return colours;
    }

    public int getNYCells() {
        return chemicalTextureHeight;
    }

    public int getNXCells() {
        return chemicalTextureWidth;
    }

    public float getPlantDensity(Vector2 pos) {
        return getDensity(pos.x, pos.y, 1);
    }

    public float getPlantDensity(float x, float y) {
        return getDensity(x, y, 1);
    }

    public float getMeatDensity(Vector2 pos) {
        return getDensity(pos.x, pos.y, 0);
    }

    public float getMeatDensity(float x, float y) {
        return getDensity(x, y, 0);
    }

    public float getDensity(float x, float y, int axis) {
        int i = toChemicalGridX(x);
        int j = toChemicalGridY(y);
        return getDensity(i, j, axis);
    }

    public float getDensity(int i, int j, int axis) {
        if (i < 0 || i >= chemicalTextureWidth || j < 0 || j >= chemicalTextureHeight)
            return 0;
;
        Colour c = colours[i][j];
        return c.get(axis) * c.a;
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

    public void depositCircle(Vector2 pos, float r, Colour c) {
        int x = toChemicalGridX(pos.x);
        int y = toChemicalGridY(-pos.y);
        int rc = toChemicalGridXDist(r);
        for (int i = -rc; i <= rc; i++) {
            for (int j = -rc; j <= rc; j++) {
                if (i*i + j*j > rc*rc)
                    continue;
                int x_ = x + i, y_ = y + j;
                set(x_, y_, c);
            }
        }
    }

    public void depositCircle(Vector2 pos, float rad, float r, float g, float b, float a) {
        int x = toChemicalGridX(pos.x);
        int y = toChemicalGridY(-pos.y);
        int rc = toChemicalGridXDist(rad);
        for (int i = -rc; i <= rc; i++) {
            for (int j = -rc; j <= rc; j++) {
                if (i*i + j*j > rc*rc)
                    continue;
                int x_ = x + i, y_ = y + j;
                set(x_, y_, r, g, b, a);
            }
        }
    }

    public float getCellSize() {
        return (getMaxX() - getMinX()) / getNXCells();
    }

    public Colour getColour(float x, float y) {
        int gridX = toChemicalGridX(x);
        int gridY = toChemicalGridY(y);
        return colours[gridX][gridY];
    }

    public Colour getColour(int i, int j) {
        return colours[i][j];
    }
}
