package com.protoevo.ui.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CellTexture {

    private static TextureAtlas textureAtlas;

    private static Sprite particleSprite;
    private static Texture particleTexture;
    private static Pixmap particleBasePixmap;
    private static BufferedImage particleImage;
    private static final String pathToFile = "cell/particle_base_512x512.png";

    public static TextureAtlas getTextureAtlas() {
        if (textureAtlas == null) {
            textureAtlas = new TextureAtlas(Gdx.files.internal("textures/cell.atlas"));
        }
        return textureAtlas;
    }

    public static Sprite getSprite() {
        if (particleSprite == null) {
            particleSprite = new Sprite(getTexture());
        }
        return particleSprite;
    }

    public static Texture getTexture() {
        if (particleTexture == null) {
//            Pixmap particlePixmap = getPixmap();
//            particleTexture = new Texture(particlePixmap, true);
//            particleTexture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);
//            particleTexture = getTextureAtlas().findRegion("particle_base_512x512").getTexture();
            particleTexture = getTextureAtlas().findRegion("base_cell").getTexture();

//            textureAtlas.findRegion()
//            particleTexture = new Texture(pathToFile);
//            particleTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.MipMapLinearLinear);
            particleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            particleTexture.setAnisotropicFilter(16);
//            particleTexture.setFilter(Texture.TextureFilter.MipMapNearestNearest, Texture.TextureFilter.MipMapNearestNearest);
        }

        return particleTexture;
    }

    public static Pixmap loadPixmap() {
        FileHandle particleFile = Gdx.files.internal(pathToFile);
        return new Pixmap(particleFile);
    }

    public static Pixmap getPixmap() {
        if (particleBasePixmap == null) {
            particleBasePixmap = loadPixmap();
        }
        return particleBasePixmap;
    }

    public static BufferedImage getBufferedImage() {
        if (particleImage == null) {
            try {
                particleImage = ImageIO.read(new File(pathToFile));
            } catch (IOException e) {
                throw new RuntimeException("Particle image not found: " + pathToFile + "\n" + e);
            }
        }
        return particleImage;
    }
}
