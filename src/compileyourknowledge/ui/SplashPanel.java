package compileyourknowledge.ui;

import compileyourknowledge.util.Backdrop;
import compileyourknowledge.util.ImageLibrary;
import compileyourknowledge.util.SoundManager;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

    /** the main menu: start, how to play, credits and exit. */

public class SplashPanel extends JPanel {

   
    private static final String SPLASH_IMAGE = "resources/images/screens/MainScreen.png";
    private static final String TITLE = "Compile Your Knowledge";

   
    private static final double CENTER_X = 0.508;
    private static final double BUTTON_WIDTH = 0.187;
    private static final double[] CENTER_Y = {0.518, 0.621, 0.722, 0.826};
    private static final double PADDING = 0.20; // extra room around each button so it can grow on hover

    private final BufferedImage background = ImageLibrary.load(SPLASH_IMAGE);
    private final ImageButton[] buttons;

    public SplashPanel(MainFrame frame) {
        setLayout(null); // doLayout() places the buttons

        ImageButton startButton = new ImageButton(
                ImageLibrary.load("resources/images/buttons/start.png"), "Start");
        ImageButton howToPlayButton = new ImageButton(
                ImageLibrary.load("resources/images/buttons/howToPlay.png"), "How to Play");
        ImageButton creditsButton = new ImageButton(
                ImageLibrary.load("resources/images/buttons/credits.png"), "Credits");
        ImageButton exitButton = new ImageButton(
                ImageLibrary.load("resources/images/buttons/exit.png"), "Exit");

        startButton.addActionListener(e -> {
            frame.showScreen(MainFrame.START); // switch screens first so a sound problem can never block it
            try {
                SoundManager.playGameplay(); // gameplay music loops from here on
            } catch (Throwable t) {
                System.err.println("Could not start gameplay music: " + t);
            }
        });
        howToPlayButton.addActionListener(e -> frame.showScreen(MainFrame.HOW_TO_PLAY));
        creditsButton.addActionListener(e -> frame.showScreen(MainFrame.CREDITS));
        exitButton.addActionListener(e -> System.exit(0));

        buttons = new ImageButton[]{startButton, howToPlayButton, creditsButton, exitButton};
        for (ImageButton button : buttons) {
            add(button);
        }
    }

    // puts the four buttons in a column using the screen size
    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        for (int i = 0; i < buttons.length; i++) {
            ImageButton button = buttons[i];
            int bw = (int) Math.round(w * BUTTON_WIDTH);
            int bh = (int) Math.round(bw * button.aspectRatio());
            int padX = (int) Math.round(bw * PADDING);
            int padY = (int) Math.round(bh * PADDING);
            int cx = (int) Math.round(w * CENTER_X);
            int cy = (int) Math.round(h * CENTER_Y[i]);

            button.setBaseSize(bw, bh);
            button.setBounds(cx - bw / 2 - padX, cy - bh / 2 - padY, bw + 2 * padX, bh + 2 * padY);
        }
    }

    // draws the background picture. if it is missing, draws the sky with the game title instead
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
            Backdrop.paint(g2, background, w, h);
        } else {
            Theme.paintSky(g2, w, h);
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 48));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(TITLE, (w - fm.stringWidth(TITLE)) / 2, h / 2);
        }
        g2.dispose();
    }
}
