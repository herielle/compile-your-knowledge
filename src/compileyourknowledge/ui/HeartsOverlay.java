package compileyourknowledge.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Draws one heart per remaining life, right aligned, on top of the scrolling text. */
final class HeartsOverlay extends JComponent {

    private final BufferedImage heartImage;
    private int lives;

    HeartsOverlay(BufferedImage heartImage) {
        this.heartImage = heartImage;
    }

    void setLives(int lives) {
        if (lives != this.lives) {
            this.lives = lives;
            repaint();
        }
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int h = getHeight();
        int w = heartImage == null ? h
                : (int) Math.round(h * (double) heartImage.getWidth() / heartImage.getHeight());
        int gap = Math.max(2, h / 24);
        for (int i = 0; i < lives; i++) {
            int x = getWidth() - (i + 1) * w - i * gap;
            if (heartImage != null) {
                g2.drawImage(heartImage, x, 0, w, h, null);
            } else {
                g2.setColor(Color.RED);
                g2.fillOval(x, 0, h, h);
            }
        }
        g2.dispose();
    }
}
