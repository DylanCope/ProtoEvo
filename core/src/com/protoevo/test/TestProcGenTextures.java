package com.protoevo.test;

import com.protoevo.ui.rendering.ProtozoaRenderer;

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
//            master = ProtozoaRenderer.generateCellImage();
////            master = ProtozoaTexture.generateImage(Evolvable.createNew(Protozoan.class));
//
//            Timer timer = new Timer(40, new ActionListener() {
//                private double time = 0;
//                private final double delta = 0.05; //.1;
//
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    time += delta;
//                    if (time > 1) {
//                        master = ProtozoaRenderer.generateCellImage();
//                        time = 0;
//                    }
//                    repaint();
//                }
//            });
//            timer.start();
        }

        @Override
        public Dimension getPreferredSize() {
            return master == null
                    ? new Dimension(500, 500)
                    : new Dimension((int) (1.5 * master.getWidth()), (int) (1.5 * master.getHeight()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0, 0, 0, 255));
            g.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D g2d = (Graphics2D) g.create();
            int x = (getWidth() - master.getWidth()) / 2;
            int y = (getHeight() - master.getHeight()) / 2;
            g2d.drawImage(master, x, y, this);
            g2d.dispose();
        }
    }


}
