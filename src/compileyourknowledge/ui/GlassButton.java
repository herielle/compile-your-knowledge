package compileyourknowledge.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** A rounded, glossy button that gives the Frutiger Aero glass look. */
public class GlassButton extends JButton {

    private boolean hovered = false;

    public GlassButton(String text) {
        super(text);
        setFont(Theme.RETRO_FONT);
        setForeground(new Color(10, 30, 80));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(220, 36));
        setMaximumSize(new Dimension(220, 36));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Color top = hovered ? new Color(255, 255, 255, 240) : new Color(255, 255, 255, 205);
        Color bottom = hovered ? new Color(150, 215, 255, 235) : new Color(120, 180, 240, 215);
        if (getModel().isPressed()) {
            top = new Color(170, 210, 245, 240);
            bottom = new Color(90, 150, 220, 240);
        }

        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fillRoundRect(0, 0, w - 1, h - 1, 24, 24);

        // Gloss highlight across the upper half.
        g2.setPaint(new Color(255, 255, 255, 90));
        g2.fillRoundRect(3, 2, w - 7, h / 2 - 2, 20, 20);

        g2.setColor(new Color(30, 80, 160, 210));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 24, 24);

        g2.dispose();
        super.paintComponent(g); // draws the button text
    }
}