package com.protoevo.test;

import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.protozoa.Protozoan;
import com.protoevo.ui.texture.ProtozoaTexture;
import com.protoevo.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class TestProcGenTextures {

    public static void main(String[] args) {
        new TestProcGenTextures();
    }

    public TestProcGenTextures() {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }

            JFrame frame = new JFrame("Testing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new TestPane());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public class TestPane extends JPanel {

        private BufferedImage master;
        private BufferedImage rotated;

        public TestPane() {
//            try {
//                master = ImageIO.read(new File("assets/entity/spike.png"));
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
            master = ProtozoaTexture.generateImage(Evolvable.createNew(Protozoan.class));
            rotated = rotateImageByDegrees(master, 0.0);

            Timer timer = new Timer(40, new ActionListener() {
                private double angle = 0;
                private final double delta = 0; //.1;

                @Override
                public void actionPerformed(ActionEvent e) {
                    angle += delta;
                    rotated = rotateImageByDegrees(master, angle);
                    repaint();
                }
            });
            timer.start();
        }

        public BufferedImage rotateImageByDegrees(BufferedImage img, double degrees) {
            return ImageUtils.rotateImageByRadians(img, Math.toRadians(degrees));
        }

        @Override
        public Dimension getPreferredSize() {
            return master == null
                    ? new Dimension(500, 500)
                    : new Dimension(master.getWidth(), master.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0, 0, 0, 255));
            g.fillRect(0, 0, getWidth(), getHeight());

//            rotated = master;
            if (rotated != null) {
//                System.out.println("Drawing rotated image");
                Graphics2D g2d = (Graphics2D) g.create();
                int x = (getWidth() - rotated.getWidth()) / 2;
                int y = (getHeight() - rotated.getHeight()) / 2;
                g2d.drawImage(rotated, x, y, this);
                g2d.dispose();
            }
        }
    }


}
