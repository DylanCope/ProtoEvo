package com.protoevo.env;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Food;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.cells.PlantCell;
import com.protoevo.biology.cells.Protozoan;
import com.protoevo.maths.Functions;
import com.protoevo.maths.Geometry;
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
    private transient JCudaKernelRunner cudaDiffusionKernel;
    private transient GLComputeShaderRunner openGLDiffusionShader;

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

            initialised = true;
        }

        // Try CUDA first if requested AND available. If the user toggled
        // useCUDA but JCuda's runtime/native libs aren't actually loadable,
        // cudaAvailable() returns false; we still want a working diffusion
        // path, so fall through to OpenGL (and finally CPU).
        boolean cudaTriedAndAvailable = false;
        if (Environment.settings.misc.useCUDA.get()
                && JCudaKernelRunner.cudaAvailable()) {
            try {
                if (DebugMode.isDebugMode())
                    System.out.println("Initialising chemical diffusion CUDA kernel...");
                cudaDiffusionKernel = new JCudaKernelRunner("diffusion");
                cudaTriedAndAvailable = true;
            } catch (Throwable t) {
                System.out.println("CUDA kernel init failed (" + t.getMessage()
                        + ") — falling back to OpenGL/CPU diffusion.");
                cudaDiffusionKernel = null;
            }
        }
        // OpenGL is the fallback whenever CUDA didn't materialize, OR
        // when the user explicitly requested OpenGL. CPU is the final
        // fallback (no init needed for cpuDiffuse).
        if (!cudaTriedAndAvailable
                && (Environment.settings.misc.useOpenGLComputeShader.get()
                    || Environment.settings.misc.useCUDA.get())) {
            try {
                openGLDiffusionShader = new GLComputeShaderRunner("diffusion");
            } catch (Throwable t) {
                System.out.println("OpenGL compute shader init failed ("
                        + t.getMessage() + ") — falling back to CPU diffusion.");
                openGLDiffusionShader = null;
            }
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
        return (int) Functions.clampedLinearRemap(x, xMin, xMax, 0, chemicalTextureWidth);
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
        return (int) Functions.clampedLinearRemap(y, yMin, yMax, 0, chemicalTextureHeight);
    }

    // Dirty flag so the renderer knows when to refresh its pixmap. Volatile
    // because sim thread writes, render thread reads. We DON'T fire a
    // per-pixel callback on every set() — at 1024×1024 with a diffuse step
    // touching every pixel, that was ~1M JNI calls into Pixmap.drawPixel
    // per chemical-update, ~30ms of pure render-thread tax. Renderer now
    // batches the whole array into the pixmap once per render frame.
    private volatile transient boolean chemicalsDirty = false;

    public boolean isDirty() { return chemicalsDirty; }
    public void clearDirty() { chemicalsDirty = false; }

    public void set(int x, int y, Colour colour) {
        if (outOfTextureBounds(x, y))
            return;
        colours[x][y].set(colour);
        chemicalsDirty = true;
        if (updateChemicalCallback != null)
            updateChemicalCallback.onChemicalUpdated(x, y, colour);
    }

    public void set(int x, int y, float r, float g, float b, float a) {
        if (outOfTextureBounds(x, y))
            return;
        colours[x][y].set(r, g, b, a);
        chemicalsDirty = true;
        if (updateChemicalCallback != null)
            updateChemicalCallback.onChemicalUpdated(x, y, colours[x][y]);
    }

    public void set(int x, int y, int rgba8888) {
        if (outOfTextureBounds(x, y))
            return;
        colours[x][y].set(rgba8888);
        chemicalsDirty = true;
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
            // Plant cells radiate a CHEMICAL HALO at ~1.8× their physical
            // radius — bigger than physical footprint to feed cells in
            // gaps between plants, but small enough that we don't paint
            // 9× more pixels per plant per tick (the 3× version was
            // ~9× the depositCircle work and made the sim feel sluggish
            // at high plant counts). 1.8× covers ~3.2× the area, enough
            // for diffusion to bridge gaps without crushing per-frame
            // throughput.
            float depositR = (e instanceof PlantCell)
                    ? e.getRadius() * 1.8f
                    : e.getRadius();
            depositCircle(
                    e.getPos(), depositR,
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
        float plantConv = Environment.settings.cell.chemicalExtractionPlantConversion.get();
        float meatConv = Environment.settings.cell.chemicalExtractionMeatConversion.get();
        float extractionFactor = Environment.settings.cell.chemicalExtractionFactor.get();

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
                        float requested = extractionFactor * delta * overlapP;
                        if (requested > 0) {
                            // Cap extraction by what's actually present per
                            // channel. Without this, batching N substeps' worth
                            // into a single call (fast-forward) silently
                            // over-credited food: the OLD colour values were
                            // multiplied into the food yield even after the
                            // pixel was about to deplete to zero. With the cap,
                            // the same batched call extracts exactly what's
                            // there — correct and delta-safe.
                            if (colour.g > 0.5f && colour.g > 1.5f * colour.r && colour.g > 1.5f * colour.b) {
                                float effG = Math.min(requested, colour.g);
                                protozoan.addFood(Food.Type.Plant,
                                        effG * colour.g * plantConv);
                            }
                            if (colour.r > 0.5f && colour.r > 1.5f * colour.g && colour.r > 1.5f * colour.b) {
                                float effR = Math.min(requested, colour.r);
                                protozoan.addFood(Food.Type.Meat,
                                        effR * colour.r * meatConv);
                            }

                            colour.sub(requested);
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
            if (cudaDiffusionKernel == null)
                initialise();

            if (cudaDiffusionKernel == null) {
                // initialise() couldn't bring CUDA back up — fall through
                // to a non-CUDA path on this tick rather than NPE.
                if (openGLDiffusionShader != null) {
                    openGLDiffuse();
                } else {
                    cpuDiffuse();
                }
                return;
            }

            cudaDiffusionKernel.processImage(
                    byteBuffer, chemicalTextureWidth, chemicalTextureHeight);
        }
        catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("CUDA_ERROR_INVALID_CONTEXT") ||
                    msg.contains("CUDA_ERROR_INVALID_HANDLE")) {
                if (DebugMode.isDebugMode())
                    System.out.println("CUDA context lost, reinitialising...");
                initialise();
            } else {
                System.out.println("CUDA diffuse failed: " + msg
                        + " — falling back to OpenGL/CPU diffusion permanently.");
                cudaDiffusionKernel = null;
                // Initialize OpenGL on first failure so the next call can use it.
                if (openGLDiffusionShader == null) {
                    try {
                        openGLDiffusionShader = new GLComputeShaderRunner("diffusion");
                    } catch (Throwable t) { /* will fall to CPU */ }
                }
            }
        }

        unloadFromByteBuffer();
    }

    private void openGLDiffuse() {
        loadIntoByteBuffer();
        if (openGLDiffusionShader == null)
            initialise();

        openGLDiffusionShader.processImage(
                byteBuffer, chemicalTextureWidth, chemicalTextureHeight);
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
        }
        // Normalize ONCE after both loops finish. Previous code did this
        // INSIDE the outer i-loop, so it fired FILTER_SIZE times (=3),
        // dividing the running accumulator by 9 and by final_alpha on each
        // pass while the inner loop kept adding new contributions. Net
        // effect: CPU-fallback diffusion output was ~1000× too small and
        // non-uniformly wrong. Only matters when CUDA + OpenGL both fail.
        float norm = (decay / final_alpha) / ((float) (FILTER_SIZE * FILTER_SIZE));
        tmp[0] *= norm;
        tmp[1] *= norm;
        tmp[2] *= norm;
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
        // Dispatch on what's actually INITIALIZED, not just on what's
        // requested in settings. If you toggle useCUDA but the kernel
        // failed to construct (drivers, missing native libs, etc.), the
        // kernel is null and we must fall back rather than NPE.
        if (Environment.settings.misc.useCUDA.get() && cudaDiffusionKernel != null)
            cudaDiffuse();
        else if (Environment.settings.misc.useOpenGLComputeShader.get()
                && openGLDiffusionShader != null)
            openGLDiffuse();
        else if (cudaDiffusionKernel != null)
            cudaDiffuse();             // implicit fallback if user setting changed mid-run
        else if (openGLDiffusionShader != null)
            openGLDiffuse();
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
