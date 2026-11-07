package compileyourknowledge.ui;

import compileyourknowledge.util.SoundManager;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * the retro pop-up: a dimmed window behind a grey dialog with a blue title bar, one or two message
 * lines, an optional score box and two buttons.
 * it covers the whole panel so nothing underneath can be clicked while it is open.
 */

final class DialogOverlay extends JComponent {

    private static final double[] DLG = {588, 470, 1120, 660};
    private static final double DLG_BTN_W = 440;
    private static final double DLG_BTN_H = 96;
    private static final double DLG_BTN_Y = 940;

    private final ArtSpace art;
    private String[] lines = {};
    private String extra;
    private int shownScore = -1; // -1 hides the score box
    private String[] labels = {};
    private Runnable[] actions = {};
    private int hover = -1;  // which button the mouse is on (-1 = none)

    DialogOverlay(ArtSpace art) {
        this.art = art;
        setVisible(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHover(hit(e.getX(), e.getY()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHover(-1);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int h = hit(e.getX(), e.getY());
                if (h >= 0 && SwingUtilities.isLeftMouseButton(e)) {
                    SoundManager.playClick(); // these buttons are painted by hand, so the click sound is played here
                    actions[h].run();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // opens the dialog. pass a negative score to hide the score box, and null extra for no extra line
    void show(String[] lines, String extra, int score, String[] labels, Runnable[] actions) {
        this.lines = lines;
        this.extra = extra;
        this.shownScore = score;
        this.labels = labels;
        this.actions = actions;
        this.hover = -1;
        setVisible(true);
        repaint();
    }

    // hides the dialog
    void close() {
        setVisible(false);
        hover = -1;
        Container parent = getParent();
        if (parent != null) {
            parent.repaint();
        }
    }

    // remembers which button is hovered and repaints only when it changes. 
    private void setHover(int h) {
        if (h != hover) {
            hover = h;
            repaint();
        }
    }

    // where button number i is, in pixels: x, y, width, height. 
    // buttons are spaced evenly across the dialog 
    private int[] buttonRect(int i) {
        double cx = DLG[0] + DLG[2] * (2 * i + 1) / (2.0 * labels.length);
        return new int[]{art.sx(cx - DLG_BTN_W / 2), art.sy(DLG_BTN_Y), art.sl(DLG_BTN_W), art.sl(DLG_BTN_H)};
    }

    // returns which button is at this point, or -1 if none.
    private int hit(int x, int y) {
        for (int i = 0; i < labels.length; i++) {
            int[] r = buttonRect(i);
            if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 120));              // dim the window behind
        g2.fillRect(0, 0, getWidth(), getHeight());

        RetroPainter.boxArt(g2, art, DLG[0], DLG[1], DLG[2], DLG[3], 6, RetroPainter.GREY);
        RetroPainter.fillArt(g2, art, DLG[0] + 6, DLG[1] + 6, DLG[2] - 12, 110, new Color(0, 0, 205));   // blue bar
        RetroPainter.fillArt(g2, art, DLG[0] + 6, DLG[1] + 116, DLG[2] - 12, 30, new Color(224, 224, 224));

        paintMessage(g2);
        paintButtons(g2);
        g2.dispose();
    }

    // the message lines, the extra line and the score box, centred between the title bar and the buttons
    private void paintMessage(Graphics2D g2) {
        double lineH = 88;
        double extraH = extra == null ? 0 : 64;
        double scoreH = shownScore < 0 ? 0 : 110;
        double block = lines.length * lineH + extraH + scoreH;
        double y = 620 + (320 - block) / 2;
        double cx = DLG[0] + DLG[2] / 2;

        g2.setColor(Color.BLACK);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(14, art.sl(lines.length == 1 ? 80 : 62))));
        for (String line : lines) {
            drawCentered(g2, line, cx, y + lineH * 0.7);
            y += lineH;
        }
        if (extra != null) {
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, art.sl(36))));
            drawCentered(g2, extra, cx, y + 40);
            y += extraH;
        }
        if (shownScore >= 0) {
            y += 20;
            RetroPainter.boxArt(g2, art, cx - 190, y, 380, 66, 4, Color.WHITE);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, art.sl(36))));
            drawCentered(g2, "SCORE : " + shownScore, cx, y + 46);
        }
    }

    // every button with a border and its label 

    private void paintButtons(Graphics2D g2) {
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, art.sl(40))));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < labels.length; i++) {
            int[] r = buttonRect(i);
            int b = Math.max(2, art.sl(5));
            g2.setColor(Color.BLACK);
            g2.fillRect(r[0], r[1], r[2], r[3]);
            g2.setColor(i == hover ? new Color(225, 225, 225) : new Color(205, 205, 205));
            g2.fillRect(r[0] + b, r[1] + b, r[2] - 2 * b, r[3] - 2 * b);
            g2.setColor(Color.WHITE);                                       // light edge on top
            g2.fillRect(r[0] + b, r[1] + b, r[2] - 2 * b, Math.max(1, b / 2));
            g2.setColor(Color.BLACK);
            g2.drawString(labels[i], r[0] + (r[2] - fm.stringWidth(labels[i])) / 2,
                    r[1] + (r[3] + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    //  text centred on an artwork x position
    private void drawCentered(Graphics2D g2, String text, double artCx, double artBaseline) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, art.sx(artCx) - fm.stringWidth(text) / 2, art.sy(artBaseline));
    }
}
