package compileyourknowledge.ui;

import compileyourknowledge.util.ImageLibrary;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** The splash screen: the intro picture as a full background with the four menu buttons on top. */
public class SplashPanel extends JPanel {

    // Change this if your file has a different name or extension (for example splash.jpg).
    private static final String SPLASH_IMAGE = "resources/images/splash.png";
    private static final String TITLE = "Compile Your Knowledge";

    private final BufferedImage background = ImageLibrary.load(SPLASH_IMAGE);

    public SplashPanel(MainFrame frame) {
        setLayout(new BorderLayout());

        GlassButton startButton = new GlassButton("Start");
        GlassButton howToPlayButton = new GlassButton("How to Play");
        GlassButton creditsButton = new GlassButton("Credits");
        GlassButton exitButton = new GlassButton("Exit");

        startButton.addActionListener(e -> frame.showScreen(MainFrame.START));
        howToPlayButton.addActionListener(e -> frame.showScreen(MainFrame.HOW_TO_PLAY));
        creditsButton.addActionListener(e -> frame.showScreen(MainFrame.CREDITS));
        exitButton.addActionListener(e -> System.exit(0));

        JPanel buttonColumn = new JPanel();
        buttonColumn.setLayout(new BoxLayout(buttonColumn, BoxLayout.Y_AXIS));
        buttonColumn.setOpaque(false);
        buttonColumn.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        GlassButton[] buttons = {startButton, howToPlayButton, creditsButton, exitButton};
        for (GlassButton button : buttons) {
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonColumn.add(button);
            buttonColumn.add(Box.createVerticalStrut(8));
        }

        add(buttonColumn, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (background != null) {
            // Scale the picture to cover the whole panel while keeping its proportions.
            double scale = Math.max((double) w / background.getWidth(), (double) h / background.getHeight());
            int drawW = (int) Math.round(background.getWidth() * scale);
            int drawH = (int) Math.round(background.getHeight() * scale);
            g2.drawImage(background, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
        } else {
            // Fallback so the title still shows if the picture is missing.
            Theme.paintSky(g2, w, h);
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 48));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(TITLE, (w - fm.stringWidth(TITLE)) / 2, h / 2);
        }
        g2.dispose();
    }
}