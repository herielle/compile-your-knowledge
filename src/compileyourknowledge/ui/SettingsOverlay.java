package compileyourknowledge.ui;

import compileyourknowledge.util.SoundManager;
import compileyourknowledge.util.SoundSettings;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Used as the window's glass pane, so it floats above every screen. It shows a small speaker button
 * in the lower right corner; pressing it (or the M key) opens a retro dialog with a music slider and a sound
 * effects slider. While the dialog is closed, only the speaker button takes mouse clicks.
 */
final class SettingsOverlay extends JComponent {

    // Everything is laid out in these "design units" and scaled to the window size.
    private static final double BASE_W = 1000;
    private static final double BASE_H = 700;

    private static final double ICON_SIZE = 46;
    private static final double ICON_MARGIN = 14;

    private static final double DLG_W = 560;
    private static final double DLG_H = 380;
    private static final double TITLE_H = 70;
    private static final double TRACK_X = 190;          // slider track start, from the dialog's left edge
    private static final double TRACK_W = 250;
    private static final double[] ROW_Y = {130, 212};   // slider centre lines, from the dialog's top edge
    private static final double BTN_W = 200;
    private static final double BTN_H = 58;

    private static final Color GREY = new Color(192, 192, 192);
    private static final Color BLUE = new Color(0, 0, 205);

    private boolean open;
    private int dragging = -1;     // 0 = music slider, 1 = effects slider
    private boolean closeHover;

    SettingsOverlay() {
        setOpaque(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                if (!open) {
                    if (iconRect().contains(e.getPoint())) {
                        SoundManager.playClick();
                        SettingsOverlay.this.open();
                    }
                    return;
                }
                if (closeRect().contains(e.getPoint())) {
                    SoundManager.playClick();
                    close();
                    return;
                }
                for (int i = 0; i < 2; i++) {
                    if (grabRect(i).contains(e.getPoint())) {
                        dragging = i;
                        setValueFromX(i, e.getX());
                        return;
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging >= 0) {
                    setValueFromX(dragging, e.getX());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging >= 0) {
                    SoundSettings.save();
                    if (dragging == 1) {
                        SoundManager.playClick(); // lets the player hear the new effects volume
                    }
                    dragging = -1;
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                boolean hover = open && closeRect().contains(e.getPoint());
                if (hover != closeHover) {
                    closeHover = hover;
                    repaint();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    boolean isOpen() {
        return open;
    }

    void open() {
        open = true;
        repaint();
    }

    void close() {
        if (open) {
            SoundSettings.save();
        }
        open = false;
        dragging = -1;
        closeHover = false;
        repaint();
    }

    void toggle() {
        if (open) {
            close();
        } else {
            open();
        }
    }

    /** Clicks reach this pane only over the speaker button, or anywhere while the dialog is open. */
    @Override
    public boolean contains(int x, int y) {
        return open || iconRect().contains(x, y);
    }

    // ------------------------------------------------------------------ geometry

    private double unit() {
        return Math.min(getWidth() / BASE_W, getHeight() / BASE_H);
    }

    private int u(double units) {
        return (int) Math.round(units * unit());
    }

    private Rectangle iconRect() {
        int size = u(ICON_SIZE);
        int margin = u(ICON_MARGIN);
        return new Rectangle(getWidth() - size - margin, getHeight() - size - margin, size, size);
    }

    private Rectangle dialogRect() {
        int w = u(DLG_W);
        int h = u(DLG_H);
        return new Rectangle((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
    }

    private Rectangle trackRect(int i) {
        Rectangle d = dialogRect();
        int h = u(14);
        return new Rectangle(d.x + u(TRACK_X), d.y + u(ROW_Y[i]) - h / 2, u(TRACK_W), h);
    }

    /** The area that starts a drag: the track plus some slack, so the thumb is easy to hit. */
    private Rectangle grabRect(int i) {
        Rectangle t = trackRect(i);
        int padX = u(14);
        int padY = u(22);
        return new Rectangle(t.x - padX, t.y - padY, t.width + 2 * padX, t.height + 2 * padY);
    }

    private Rectangle closeRect() {
        Rectangle d = dialogRect();
        int w = u(BTN_W);
        int h = u(BTN_H);
        return new Rectangle(d.x + (d.width - w) / 2, d.y + d.height - h - u(32), w, h);
    }

    private float value(int i) {
        return i == 0 ? SoundSettings.getMusic() : SoundSettings.getEffects();
    }

    private void setValueFromX(int i, int x) {
        Rectangle t = trackRect(i);
        float v = (float) (x - t.x) / t.width;
        v = Math.max(0f, Math.min(1f, v));
        if (i == 0) {
            SoundSettings.setMusic(v);
        } else {
            SoundSettings.setEffects(v);
        }
        repaint();
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paintIcon(g2);
        if (open) {
            paintDialog(g2);
        }
        g2.dispose();
    }

    private void paintIcon(Graphics2D g2) {
        Rectangle r = iconRect();
        box(g2, r.x, r.y, r.width, r.height, Math.max(2, u(3)), GREY);

        // A little speaker: body, cone and two sound waves.
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        int s = r.width;
        g2.setColor(Color.BLACK);
        g2.fillRect(cx - s * 28 / 100, cy - s * 10 / 100, s * 12 / 100, s * 20 / 100);
        Polygon cone = new Polygon(
                new int[]{cx - s * 16 / 100, cx + s * 2 / 100, cx + s * 2 / 100, cx - s * 16 / 100},
                new int[]{cy - s * 10 / 100, cy - s * 24 / 100, cy + s * 24 / 100, cy + s * 10 / 100}, 4);
        g2.fillPolygon(cone);
        g2.setStroke(new BasicStroke(Math.max(2f, s / 16f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        boolean silent = SoundSettings.getMusic() <= 0f && SoundSettings.getEffects() <= 0f;
        if (silent) {
            g2.setColor(new Color(200, 0, 0));
            g2.drawLine(cx + s * 8 / 100, cy - s * 14 / 100, cx + s * 30 / 100, cy + s * 14 / 100);
            g2.drawLine(cx + s * 30 / 100, cy - s * 14 / 100, cx + s * 8 / 100, cy + s * 14 / 100);
        } else {
            int w1 = s * 14 / 100;
            int w2 = s * 28 / 100;
            g2.drawArc(cx + s * 2 / 100 - w1 / 2, cy - w1, w1, w1 * 2, -50, 100);
            g2.drawArc(cx + s * 2 / 100 - w2 / 2, cy - w2, w2, w2 * 2, -50, 100);
        }
    }

    private void paintDialog(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 140));               // dim the game behind
        g2.fillRect(0, 0, getWidth(), getHeight());

        Rectangle d = dialogRect();
        box(g2, d.x, d.y, d.width, d.height, Math.max(2, u(6)), GREY);
        int bar = Math.max(2, u(6));
        g2.setColor(BLUE);
        g2.fillRect(d.x + bar, d.y + bar, d.width - 2 * bar, u(TITLE_H) - bar);

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, u(32))));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("Sound", d.x + u(28), d.y + u(TITLE_H) / 2 + (fm.getAscent() - fm.getDescent()) / 2 + bar / 2);

        String[] labels = {"Music", "Effects"};
        for (int i = 0; i < 2; i++) {
            paintSlider(g2, i, labels[i]);
        }

        Rectangle b = closeRect();
        int border = Math.max(2, u(5));
        g2.setColor(Color.BLACK);
        g2.fillRect(b.x, b.y, b.width, b.height);
        g2.setColor(closeHover ? new Color(225, 225, 225) : new Color(205, 205, 205));
        g2.fillRect(b.x + border, b.y + border, b.width - 2 * border, b.height - 2 * border);
        g2.setColor(Color.WHITE);
        g2.fillRect(b.x + border, b.y + border, b.width - 2 * border, Math.max(1, border / 2));
        g2.setColor(Color.BLACK);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, u(26))));
        fm = g2.getFontMetrics();
        g2.drawString("Close", b.x + (b.width - fm.stringWidth("Close")) / 2,
                b.y + (b.height + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void paintSlider(Graphics2D g2, int i, String label) {
        Rectangle d = dialogRect();
        Rectangle t = trackRect(i);
        float v = value(i);
        int centreY = t.y + t.height / 2;

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, u(26))));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Color.BLACK);
        g2.drawString(label, d.x + u(36), centreY + (fm.getAscent() - fm.getDescent()) / 2);

        // Sunken track with the filled part in blue.
        int border = Math.max(2, u(3));
        box(g2, t.x, t.y, t.width, t.height, border, Color.WHITE);
        int filled = (int) Math.round((t.width - 2 * border) * v);
        g2.setColor(BLUE);
        g2.fillRect(t.x + border, t.y + border, filled, t.height - 2 * border);

        // Thumb.
        int thumbW = u(22);
        int thumbH = u(40);
        int thumbX = t.x + (int) Math.round(t.width * v) - thumbW / 2;
        box(g2, thumbX, centreY - thumbH / 2, thumbW, thumbH, border, GREY);

        String percent = Math.round(v * 100) + "%";
        g2.setColor(Color.BLACK);
        g2.drawString(percent, t.x + t.width + u(26), centreY + (fm.getAscent() - fm.getDescent()) / 2);
    }

    /** A black-bordered box: the border first, then the fill inside it. */
    private static void box(Graphics2D g2, int x, int y, int w, int h, int border, Color fill) {
        g2.setColor(Color.BLACK);
        g2.fillRect(x, y, w, h);
        g2.setColor(fill);
        g2.fillRect(x + border, y + border, w - 2 * border, h - 2 * border);
    }
}
