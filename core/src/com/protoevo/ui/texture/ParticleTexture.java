package com.protoevo.ui.texture;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ParticleTexture {

    private static Texture particleTexture;
    private static Pixmap particleBasePixmap;
    private static BufferedImage particleImage;
    private static String pathToFile = "entity/particle_base_128x128.png";

    public static Texture getTexture() {
        if (particleTexture == null) {
            Pixmap particlePixmap = getPixmap();
            particleTexture = new Texture(particlePixmap, true);
            particleTexture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);
        }

        return particleTexture;
    }

    public static Pixmap getPixmap() {
        if (particleBasePixmap == null) {
            FileHandle particleFile = Gdx.files.internal(pathToFile);
            particleBasePixmap = new Pixmap(particleFile);
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
