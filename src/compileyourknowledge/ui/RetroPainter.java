package compileyourknowledge.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/** Drawing helpers for the retro "grey window" look, all in artwork coordinates. */
final class RetroPainter {

    static final Color GREY = new Color(192, 192, 192);

    private RetroPainter() {
    }

    static void fillArt(Graphics2D g2, ArtSpace art, double x, double y, double w, double h, Color c) {
        g2.setColor(c);
        g2.fillRect(art.sx(x), art.sy(y), art.sl(w), art.sl(h));
    }

    /** A black-bordered box: the border is drawn first, then the fill inset by the border width. */
    static void boxArt(Graphics2D g2, ArtSpace art, double x, double y, double w, double h,
                       double border, Color fill) {
        fillArt(g2, art, x, y, w, h, Color.BLACK);
        fillArt(g2, art, x + border, y + border, w - 2 * border, h - 2 * border, fill);
    }

    /** The retro window from the mockup. */
    static void paintWindow(Graphics2D g2, ArtSpace art) {
        boxArt(g2, art, 80, 145, 2136, 1325, 6, GREY);                          // outer frame
        fillArt(g2, art, 108, 175, 2080, 122, new Color(18, 0, 120));           // title bar
        boxArt(g2, art, 106, 323, 2084, 1121, 6, Color.WHITE);                  // white inner area
        boxArt(g2, art, 163, 360, 1970, 772, 12, GREY);                         // question panel
        fillArt(g2, art, 175, 372, 1946, 12, new Color(222, 222, 222));         // top highlight

        // Minimize, maximize, close boxes.
        double[] bx = {1907, 2000, 2095};
        for (double x : bx) {
            fillArt(g2, art, x, 205, 62, 62, GREY);
        }
        fillArt(g2, art, 1925, 245, 28, 8, Color.BLACK);                        // minimize bar
        boxArt(g2, art, 2018, 222, 26, 26, 6, GREY);                            // maximize box
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(Math.max(2f, (float) (7 * art.scale()))));
        g2.drawLine(art.sx(2112), art.sy(222), art.sx(2142), art.sy(250));      // close X
        g2.drawLine(art.sx(2142), art.sy(222), art.sx(2112), art.sy(250));
    }
}
