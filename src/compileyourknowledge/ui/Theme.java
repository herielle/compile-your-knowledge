package compileyourknowledge.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;

/** Shared colors, fonts, and the sky-and-hills gradient used behind the info screens. */
public final class Theme {

    public static final Color TITLE_BAR = new Color(0, 0, 128);
    public static final Font RETRO_FONT = new Font(Font.MONOSPACED, Font.BOLD, 16);

    private Theme() {
    }

    public static void paintSky(Graphics2D g2, int width, int height) {
        if (height <= 0) {
            return;
        }
        float[] stops = {0f, 0.55f, 0.56f, 1f};
        Color[] colors = {
                new Color(70, 150, 230),   // upper sky
                new Color(200, 230, 255),  // horizon
                new Color(120, 200, 80),   // hilltop
                new Color(50, 130, 40)     // lower hills
        };
        g2.setPaint(new LinearGradientPaint(0, 0, 0, height, stops, colors));
        g2.fillRect(0, 0, width, height);
    }
}