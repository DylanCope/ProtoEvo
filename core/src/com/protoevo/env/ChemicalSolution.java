package com.protoevo.env;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.biology.Cell;
import com.protoevo.biology.PlantCell;
import com.protoevo.core.settings.Settings;
import com.protoevo.utils.Utils;

import java.io.Serializable;

public class ChemicalSolution implements Serializable {
    public static final long serialVersionUID = 1L;

    private final Environment environment;
    private final float cellSizeX, cellSizeY;
    private final float xMin, yMin, xMax, yMax;
    private final int chemicalTextureHeight;
    private final int chemicalTextureWidth;
    private Pixmap depositPixmap;
    private Texture chemicalTexture;
    private TextureRegion textureRegion;
    private Sprite chemicalSprite;
    private final FrameBuffer chemicalFrameBuffer;
    private final SpriteBatch batch;
    private final ShaderProgram diffuseShader;
    private float timeSinceUpdate = 0;

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

        depositPixmap = new Pixmap(cells, cells, Pixmap.Format.RGBA8888);
        depositPixmap.setColor(0, 0, 0, 0);

        chemicalTexture = new Texture(depositPixmap);
        chemicalFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888,
                cells, cells, false);

        batch = new SpriteBatch();

        diffuseShader = new ShaderProgram(
                Gdx.files.internal("shaders/diffuse/vertex.glsl"),
                Gdx.files.internal("shaders/diffuse/fragment.glsl"));
        if (!diffuseShader.isCompiled()) {
            System.err.println(diffuseShader.getLog());
            System.exit(1);
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

    public int toChemicalGridY(float y) {
        return (int) Utils.linearRemap(y, yMin, yMax, 0, chemicalTextureHeight);
    }

    public void depositChemicals(float delta, Cell e) {
        if (e instanceof PlantCell && !e.isDead()) {
//            int i = toChemicalGridX(e.getPos().x);
//            int j = toChemicalGridY(-e.getPos().y);
            float k = Settings.plantPheromoneDeposit;
            float deposit = .5f; // Math.min(1f, delta * k * e.getRadius() * e.getHealth());
//            float deposit = delta * 1000f;
            Color cellColour = e.getColor();
//            System.out.println("depositing " + deposit + " at " + e.getPos()  + " ( " + i + ", " + j + ")");
            depositPixmap.setColor(cellColour.r, cellColour.g, cellColour.b, deposit);
//            int i = toChemicalGridX(e.getPos().x);
//            int j = toChemicalGridY(-e.getPos().y);
//            depositPixmap.drawPixel(i, j);

            int i = toChemicalGridX(e.getPos().x);
            int j = toChemicalGridY(-e.getPos().y);
            int r = toChemicalGridXDist(e.getRadius() * 0.8f);
            depositPixmap.fillCircle(i, j, r);

        }
    }

    public void deposit() {
//        depositPixmap.setColor(0, 0, 0, 0);
//        depositPixmap.fill();
        environment.getCells()
                .forEach(e -> depositChemicals(timeSinceUpdate, e));
        chemicalTexture.draw(depositPixmap, 0, 0);
    }

    private void bindShader() {
        diffuseShader.bind();
        diffuseShader.setUniformf("u_delta", timeSinceUpdate);
        diffuseShader.setUniformf("u_resolution", chemicalTextureWidth, chemicalTextureHeight);

//        Gdx.graphics.getGL20().glActiveTexture(GL20.GL_TEXTURE0);
//        chemicalTexture.bind(0);
//        diffuseShader.setUniformi("u_texture_pos", 0);

        batch.setShader(diffuseShader);
    }

    public void diffuse() {
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        chemicalFrameBuffer.bind();
        chemicalFrameBuffer.begin();
//        batch.enableBlending();
        batch.begin();
        batch.draw(chemicalTexture, 0, 0, chemicalTextureWidth, chemicalTextureHeight);
        batch.end();

        chemicalFrameBuffer.end();
//
        chemicalTexture = chemicalFrameBuffer.getColorBufferTexture();
//        Pixmap pixmap = chemicalTexture.getTextureData().consumePixmap();
//        chemicalSprite = new Sprite(chemicalFrameBuffer.getColorBufferTexture());
//        chemicalSprite.flip(false, true);
//        textureRegion = new TextureRegion(chemicalFrameBuffer.getColorBufferTexture());
//        textureRegion.flip(false, true);
//        chemicalTexture = textureRegion.getTexture();
//        chemicalTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
//        batch.setShader(null);
    }

    public void update(float delta) {
        timeSinceUpdate += delta;
//        if (timeSinceUpdate >= Settings.chemicalUpdateTime) {
            deposit();
//            diffuse();
            timeSinceUpdate = 0;
//        }
    }

    public void render(OrthographicCamera camera) {
//        diffuse();
//        bindShader();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(chemicalTexture,
                0, 0,
                Gdx.graphics.getHeight(), Gdx.graphics.getHeight());

        float x = -getFieldWidth() / 2;
        float y = -getFieldHeight() / 2;
        batch.draw(chemicalTexture, x, y, getFieldWidth(), getFieldWidth());
        batch.end();
    }

    public Texture getChemicalTexture() {
        return chemicalTexture;
    }

//    public float getPlantPheromoneGradientX(int i, int j) {
//        if (i < 1 || i >= nXChunks - 1)
//            return 0f;
//        return chemicalTexture[i-1][j].currentPlantPheromoneDensity - chemicalTexture[i+1][j].currentPlantPheromoneDensity;
//    }
//
//    public float getPlantPheromoneGradientY(int i, int j) {
//        if (j < 1 || j >= nYChunks - 1)
//            return 0f;
//        return chemicalTexture[i][j-1].currentPlantPheromoneDensity - chemicalTexture[i][j+1].currentPlantPheromoneDensity;
//    }

    public int getNYChunks() {
        return chemicalTextureHeight;
    }

    public int getNXChunks() {
        return chemicalTextureWidth;
    }

    public float getPlantPheromoneDensity(int i, int j) {
        Color depositColour = new Color(depositPixmap.getPixel(i, j));
        return depositColour.g;
    }
}
