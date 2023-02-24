package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.protoevo.utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CellTexture {

    private TextureAtlas textureAtlas;
    private Sprite particleSprite;
    private Texture particleTexture;
    private Pixmap particleBasePixmap;
    private static final String pathToFile = "cell/particle_base_512x512.png";

    private static final CellTexture instance = new CellTexture();

    private CellTexture() {}

    public static void dispose() {
        if (instance.textureAtlas != null)
            instance.textureAtlas.dispose();
        instance.textureAtlas = null;
        if (instance.particleSprite != null)
            instance.particleSprite.getTexture().dispose();
        instance.particleSprite = null;
        if (instance.particleTexture != null)
           instance.particleTexture.dispose();
        instance.particleTexture = null;
        if (instance.particleBasePixmap != null)
            instance.particleBasePixmap.dispose();
        instance.particleBasePixmap = null;
    }

    public static TextureAtlas getTextureAtlas() {
        if (instance.textureAtlas == null) {
            instance.textureAtlas = new TextureAtlas(Gdx.files.internal("textures/cell.atlas"));
        }
        return instance.textureAtlas;
    }

    public static Sprite getSprite() {
        if (instance.particleSprite == null) {
            instance.particleSprite = new Sprite(getTexture());
        }
        return instance.particleSprite;
    }

    public static Texture getTexture() {
        if (instance.particleTexture == null) {
            instance.particleTexture = ImageUtils.getTexture(pathToFile);
        }

        return instance.particleTexture;
    }

    public static Pixmap loadPixmap() {
        FileHandle particleFile = Gdx.files.internal(pathToFile);
        return new Pixmap(particleFile);
    }

    public static Pixmap getPixmap() {
        if (instance.particleBasePixmap == null) {
            instance.particleBasePixmap = loadPixmap();
        }
        return instance.particleBasePixmap;
    }
}
