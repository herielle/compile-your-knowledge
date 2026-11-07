package compileyourknowledge.ui;

import compileyourknowledge.util.Backdrop;
import compileyourknowledge.util.ImageLibrary;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A full-screen picture (2000x1414 artwork) with invisible clickable areas on top of it.
 * How to Play and both Credits pages use this: the window, text, and arrow are all in the
 * artwork, and the hotspots sit exactly over the X in the title bar and the arrow button.
 */
public class ScreenPanel extends JPanel {

    private static final double ART_W = 2000;
    private static final double ART_H = 1414;

    // The X in the title bar, drawn in the artwork (same spot on every screen).
    private static final double CLOSE_X = 1824;
    private static final double CLOSE_Y = 181;
    private static final double CLOSE_SIZE = 54;

    private static final class Hotspot {
        final JButton button;
        final double x, y, w, h;

        Hotspot(JButton button, double x, double y, double w, double h) {
            this.button = button;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private final BufferedImage background;
    private final List<Hotspot> hotspots = new ArrayList<>();

    public ScreenPanel(MainFrame frame, String imagePath) {
        background = ImageLibrary.load(imagePath);
        setLayout(null);
        addHotspot(CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE, "Back to menu",
                () -> frame.showScreen(MainFrame.SPLASH));
    }

    /** Adds an invisible button over the given rectangle (in 2000x1414 artwork coordinates). */
    public final void addHotspot(double x, double y, double w, double h, String tooltip, Runnable action) {
        JButton button = new JButton();
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        button.addActionListener(e -> action.run());
        hotspots.add(new Hotspot(button, x, y, w, h));
        add(button);
    }

    @Override
    public void doLayout() {
        double scale = Math.max(getWidth() / ART_W, getHeight() / ART_H);
        double offsetX = (getWidth() - ART_W * scale) / 2.0;
        double offsetY = (getHeight() - ART_H * scale) / 2.0;
        for (Hotspot h : hotspots) {
            h.button.setBounds(
                    (int) Math.round(offsetX + h.x * scale),
                    (int) Math.round(offsetY + h.y * scale),
                    (int) Math.round(h.w * scale),
                    (int) Math.round(h.h * scale));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();
        if (background != null) {
            Backdrop.paint(g2, background, w, h); // scaled once per size, not on every repaint
        } else {
            Theme.paintSky(g2, w, h);
        }
        g2.dispose();
    }
}
