package com.protoevo.ui.texture;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.core.Simulation;
import com.protoevo.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ProtozoaTexture extends Texture {

    private static Canvas canvas;
    private static BufferedImage nodeEmpty;

    enum InteriorTexture {
        GOLGI(1, 2, "golgi"),
        NUCLEUS(1, 1, "nucleus_1", "nucleus_2"),
        RIBOSOMES(0, 2, "ribosomes_1", "ribosomes_2", "ribosomes_3");

        private final BufferedImage[] textures;
        private final int minCount, maxCount;
        InteriorTexture(int minCount, int maxCount, String...textureNames) {
            this.minCount = minCount;
            this.maxCount = maxCount;
            textures = new BufferedImage[textureNames.length];
            for (int i = 0; i < textureNames.length; i++) {
                textures[i] = ImageUtils.loadImage("entity/cell_interior/" + textureNames[i] + ".png");
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

    public ProtozoaTexture(Protozoan protozoan) {
        super(generatePixmap(protozoan));
    }

    public static Pixmap generatePixmap(Protozoan protozoan) {
        BufferedImage image = generateImage(protozoan);
        return ImageUtils.convertToPixmap(image);
    }

    public static BufferedImage generateCellImage() {
        BufferedImage base = ParticleTexture.getBufferedImage();

        ArrayList<InteriorTexture> types = new ArrayList<>();
        Simulation.RANDOM = new Random(1);
        for (InteriorTexture type : InteriorTexture.values()) {
            int count = Simulation.RANDOM.nextInt(type.getMinCount(),  type.getMaxCount() + 1);
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
        for (int i = 0; i < types.size(); i++) {
            double t = 2*Math.PI * (double) i / types.size();
            BufferedImage texture = ImageUtils.rotateImageByRadians(types.get(i).randomTexture(), t);
            if (Simulation.RANDOM.nextBoolean())
                texture = ImageUtils.flipImageHorizontally(texture);
            if (Simulation.RANDOM.nextBoolean())
                texture = ImageUtils.flipImageVertically(texture);

            g2d.drawImage(texture, null, 0, 0);
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
