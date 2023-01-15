package com.protoevo.ui.texture;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.core.Simulation;
import com.protoevo.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ProtozoaRenderer {

    enum InteriorTexture {
        GOLGI(0, 2, "golgi", "golgi_2", "golgi_3"),
        NUCLEUS(1, 1, "nucleus_1", "nucleus_2"),
        RIBOSOMES(0, 3, "ribosomes_1", "ribosomes_2", "ribosomes_3"),
        MITOCHONDRIA(0, 2, "mitochondria_1"),
        MICROFILAMENT(0, 3, "microfilament_1", "microfilament_2", "microfilament_3");

        private final BufferedImage[] textures;
        private final int minCount, maxCount;
        InteriorTexture(int minCount, int maxCount, String...textureNames) {
            this.minCount = minCount;
            this.maxCount = maxCount;
            textures = new BufferedImage[textureNames.length];
            for (int i = 0; i < textureNames.length; i++) {
                textures[i] = ImageUtils.loadImage("cell/cell_interior/" + textureNames[i] + ".png");
            }
        }

        public int getMaxCount() {
            return maxCount;
        }

        public int getMinCount() {
            return minCount;
        }

        public BufferedImage randomTexture() {
            return textures[Simulation.RANDOM.nextInt(0, textures.length)];
        }
    }

    private final Protozoan protozoan;
    private final Sprite cellSprite;
    private static Sprite nodeEmptySprite;

    public ProtozoaRenderer(Protozoan protozoan) {
        this.protozoan = protozoan;
        cellSprite = ImageUtils.convertToSprite(generateCellImage());
        if (nodeEmptySprite == null) {
            nodeEmptySprite = ImageUtils.loadSprite("cell/surface_node_empty.png");
//            nodeEmptySprite = ImageUtils.loadSprite("cell/spike/spike_large.png");
        }
    }

    public void render(float delta, SpriteBatch batch) {
        Vector2 pos = protozoan.getPos();
        float x = pos.x - protozoan.getRadius();
        float y = pos.y - protozoan.getRadius();
        float cellAngle = (float) Math.toDegrees(protozoan.getBody().getAngle());
        float size = protozoan.getRadius() * 2;

        nodeEmptySprite.setColor(protozoan.getColor());
        nodeEmptySprite.setPosition(pos.x - size, pos.y - size);
        nodeEmptySprite.setSize(2*size, 2*size);
        nodeEmptySprite.setOriginCenter();
        int n = 8;
        float dt = 360f / n;
        for (int i = 0; i < n; i++) {
            float angle = i * dt;
            nodeEmptySprite.setRotation(cellAngle + angle);
            nodeEmptySprite.draw(batch);
        }

        cellSprite.setColor(protozoan.getColor());
        cellSprite.setOriginCenter();
        cellSprite.setRotation(cellAngle);
        cellSprite.setPosition(x, y);
        cellSprite.setSize(size, size);
        cellSprite.draw(batch);
    }

    public boolean isStale() {
        return protozoan.isDead();
    }

    public static Pixmap generatePixmap(Protozoan protozoan) {
        BufferedImage image = generateImage(protozoan);
        return ImageUtils.convertToPixmap(image);
    }

    public static BufferedImage generateCellImage() {
        BufferedImage base = ParticleTexture.getBufferedImage();

        ArrayList<InteriorTexture> types = new ArrayList<>();
        Random random = new Random();
        for (InteriorTexture type : InteriorTexture.values()) {
            int count = random.nextInt(type.getMinCount(),  type.getMaxCount() + 1);
            for (int i = 0; i < count; i++) {
                types.add(type);
            }
        }
        Collections.shuffle(types);

        BufferedImage newImage = new BufferedImage(
                base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = newImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(base, null, 0, 0);
        double dt = 2 * Math.PI / types.size();
        for (int i = 0; i < types.size(); i++) {
            double t = i * dt + random.nextDouble(-dt / 3, dt / 3);
            BufferedImage texture = types.get(i).randomTexture();
            texture = ImageUtils.scaleImage(texture, random.nextDouble(0.6, 1));
//            if (random.nextBoolean())
//                texture = ImageUtils.flipImageHorizontally(texture);
            texture = ImageUtils.rotateImageByRadians(texture, t);
//            if (random.nextBoolean())
//                texture = ImageUtils.flipImageVertically(texture);

            g2d.drawImage(texture, null,
                    (newImage.getWidth() - texture.getWidth()) / 2,
                    (newImage.getHeight() - texture.getHeight()) / 2);
        }
        return newImage;
    }

    private static BufferedImage renderOnCircumference(
            BufferedImage image, double radius, double radians) {
        int newWidth = (int) (2*radius + 2*image.getHeight());
        int newHeight = (int) (2*radius + 2*image.getHeight());
        BufferedImage newImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = newImg.createGraphics();

        int centerX = newWidth / 2;
        int centerY = newHeight / 2;
        g2d.drawImage(image, null,
                (int) (centerX - image.getWidth() / 2),
                (int) (centerY - radius - image.getHeight()));
        g2d.dispose();
        return ImageUtils.rotateImageByRadians(newImg, radians);
    }

    public static BufferedImage generateImage(Protozoan protozoan) {
        BufferedImage particleImage = ParticleTexture.getBufferedImage();
        int particleWidth = particleImage.getWidth();
        int particleHeight = particleImage.getHeight();

        BufferedImage nodeEmptyImage = null;
        int nodeEmptyWidth = nodeEmptyImage.getWidth();
        int nodeEmptyHeight = nodeEmptyImage.getHeight();

        int textureWidth = particleWidth + 2*nodeEmptyHeight;
        int textureHeight = particleHeight + 2*nodeEmptyHeight;
        BufferedImage protozoaTexture = new BufferedImage(
                textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = protozoaTexture.createGraphics();
        Canvas canvas = new Canvas();
        int nNodes = 10;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        double centreX = textureWidth / 2.;
        double centreY = textureHeight / 2.;
        double r = particleWidth / 2.0;
        double halfWidth = nodeEmptyWidth / 2.0;
        double deltaT = Math.asin(halfWidth / r);

        double particleX = centreX - r;
        double particleY = centreY - r;

        r = 0.95 * r;

        int dotR = 2;
        for (int node = 0; node < nNodes; node++) {
//            double t = Math.PI / 2 - node * 2 * Math.PI / nNodes;
            double t = node * 2 * Math.PI / nNodes;
            System.out.println(t);

//            BufferedImage rotatedNode = renderOnCircumference(nodeEmptyImage, r, t);

            BufferedImage rotatedNode =
                    ImageUtils.rotateImageByRadians(nodeEmptyImage, t);
            int x = (int) (centreX - rotatedNode.getWidth() / 2);
            int y = (int) (centreY - rotatedNode.getHeight() / 2);

//            BufferedImage rotatedNode =
//                    ImageUtils.rotateImageByRadians(nodeEmptyImage, Math.PI / 2 + t);
//            double x, y;
//            if (t == 0) {
//                x = centreX + r;
//                y = centreY - halfWidth;
//            }
//            else if (t < Math.PI / 2) {
//                System.out.println(1);
//                x = centreX + r * Math.cos(t - deltaT) - nodeEmptyWidth * Math.cos(t);
//                y = centreY + r * Math.sin(t - deltaT);
//            }
//            else if (t <= Math.PI) {
//                System.out.println(2);
//                t = Math.PI - t;
//                x = centreX - r * Math.cos(t - deltaT) - nodeEmptyHeight * Math.cos(t);
//                y = centreY + r * Math.sin(t - deltaT);
//            }
//            else if (t < 3 * Math.PI / 2) {
//                System.out.println(3);
//                t = 3 * Math.PI / 2 - t;
//                x = centreX - r * Math.sin(t + deltaT) - nodeEmptyHeight * Math.sin(t);
//                y = centreY - r * Math.cos(t - deltaT) - nodeEmptyHeight * Math.sin(t);
//            }
//            else {
//                System.out.println(4);
//                t = 2*Math.PI - t;
//                x = centreX + r * Math.cos(t + deltaT);
//                y = centreY - r * Math.sin(t + deltaT) - nodeEmptyHeight * Math.sin(t);
//            }
//            else { // if (t <= 2 * Math.PI) {
//                x = centreX;
//                y = centreY;
//            }
//            System.out.println(x + ", " + y);
//            g2d.setComposite(new ColorConvertOp(ColorSp));
            g2d.drawImage(rotatedNode, null, (int) x, (int) y);
        }

//        g2d.setColor(Color.RED);
//        g2d.fillOval(
//                (int) - dotR,
//                (int) - dotR,
//                2*dotR, 2*dotR);
//
//        g2d.setColor(Color.BLUE);
//        g2d.fillOval(
//                (int) (centreX - dotR),
//                (int) (centreX - dotR),
//                2*dotR, 2*dotR);

//        float alpha = 0.5f;
////        float alpha = 1.f;
//        int type = AlphaComposite.SRC_OVER;
//        AlphaComposite composite =
//                AlphaComposite.getInstance(type, alpha);
//        g2d.setComposite(composite);
        g2d.drawImage(particleImage, (int) particleX, (int) particleY, canvas);

        return protozoaTexture;
    }
}
