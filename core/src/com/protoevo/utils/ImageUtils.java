package com.protoevo.utils;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.google.common.collect.Lists;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageUtils {

    private static final Canvas canvas = new Canvas();

    public static void drawOnCircumference(
            SpriteBatch batch, Sprite texture, Vector2 center,
            float radius, float angle, float width) {
//        Vector2 nodePos = node.getRelativePos().cpy().scl(0.95f).add(center);

        float nodeX = (float) (center.x + radius * Math.cos(angle));
        float nodeY = (float) (center.y + radius * Math.sin(angle));

        float h = width * texture.getHeight() / texture.getWidth();
        texture.setBounds(nodeX - width/2f, nodeY, width, h);

        texture.setOrigin(width/2f, 0);
        texture.setRotation((float) Math.toDegrees(angle) - 90);

        texture.draw(batch);
    }

    public static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int convertARGBToRGBA(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = (argb) & 0xFF;
        return (r << 24) | (g << 16) | (b << 8) | a;
    }

    public static Sprite loadSprite(String path) {
        return new Sprite(getTexture(path));
    }

    public static Sprite convertToSprite(BufferedImage image) {
        Sprite sprite = new Sprite(convertToTexture(image));
        sprite.setOriginCenter();
        return sprite;
    }

    public static Texture convertToTexture(BufferedImage image) {
        Texture texture = new Texture(convertToPixmap(image), true);
//        texture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.MipMapLinearNearest);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    public static Pixmap convertToPixmap(BufferedImage image) {
        Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGBA8888);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                int rgba = convertARGBToRGBA(argb);
                pixmap.drawPixel(x, y, rgba);
            }
        }
        return pixmap;
    }

    public static BufferedImage rotateImageByRadians(BufferedImage img, double rads) {
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2f, (newHeight - h) / 2f);

        double x = w / 2.;
        double y = h / 2.;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(img, null, 0, 0);
        g2d.dispose();

        return rotated;
    }

    public static BufferedImage scaleImage(BufferedImage img, double sx, double sy) {

        BufferedImage scaledImg = new BufferedImage(
                (int) Math.abs(img.getWidth() * sx), (int) Math.abs(img.getHeight() * sx),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        AffineTransform at = new AffineTransform();
//        at.translate(img.getWidth(), 0);
        at.scale(sx, sy);
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, canvas);
        g2d.dispose();

        return scaledImg;
    }

    public static BufferedImage scaleImage(BufferedImage img, double s) {
        return scaleImage(img, s, s);
    }

    public static BufferedImage flipImageHorizontally(BufferedImage img) {
        return scaleImage(img, -1, 1);
    }

    public static BufferedImage flipImageVertically(BufferedImage img) {
        return scaleImage(img, 1, -1);
    }

    public static Sprite[] loadSpriteAnimationFrames(String framesFolder) {
        int frame = 1;
        List<Sprite> frames = Lists.newArrayList();
        while (true) {
            String framePath = framesFolder + "/" + frame + ".png";
            File frameFile = new File(framePath);
            if (!frameFile.exists()) {
                break;
            }
            frames.add(loadSprite(framePath));
            frame++;
        }
        return frames.toArray(new Sprite[0]);
    }

    public static BufferedImage[] loadAnimationFrames(String framesFolder) {
        int frame = 1;
        List<BufferedImage> frames = Lists.newArrayList();
        while (true) {
            String framePath = framesFolder + "/" + frame + ".png";
            File frameFile = new File(framePath);
            if (!frameFile.exists()) {
                break;
            }
            frames.add(loadImage(framePath));
            frame++;
        }
        return frames.toArray(new BufferedImage[0]);
    }

    public static Texture getTexture(String s) {
        Texture texture =  new Texture(s);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texture.setAnisotropicFilter(16);
        return texture;
    }
}
