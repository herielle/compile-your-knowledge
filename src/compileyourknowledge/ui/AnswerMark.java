package compileyourknowledge.ui;

import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** A small overlay that draws the correct check or the wrong X on top of a letter button. */
final class AnswerMark extends JComponent {

    private BufferedImage image;

    void setImage(BufferedImage img) {
        if (img != image) {
            image = img;
            repaint();
        }
    }

    @Override
    public boolean contains(int x, int y) {
        return false; // clicks pass straight through to the letter button below
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        g2.dispose();
    }
}
