package compileyourknowledge.ui;

import compileyourknowledge.util.ImageLibrary;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class PlayPanel extends JPanel {

    private static final String PLAY_IMAGE = "resources/images/PlayScreen.png";

    private static final double ART_W = 2000;
    private static final double ART_H = 1414;

    // Centers and width of the two buttons in the artwork.
    private static final double BUTTON_CENTER_X = 1000;
    private static final double BUTTON_WIDTH = 956;
    private static final double[] BUTTON_CENTER_Y = {631, 886};

    // The X in the title bar, drawn in the artwork. An invisible button sits on top of it.
    private static final double CLOSE_X = 1824;
    private static final double CLOSE_Y = 178;
    private static final double CLOSE_SIZE = 54;

    private static final double PADDING = 0.20; // room around each button for the hover grow

    private final BufferedImage background = ImageLibrary.load(PLAY_IMAGE);
    private final ImageButton theoreticalButton;
    private final ImageButton practicalButton;
    private final JButton closeButton;

    public PlayPanel(MainFrame frame) {
        setLayout(null); // positioned by doLayout() below

        theoreticalButton = new ImageButton(
                ImageLibrary.load("resources/images/buttons/theoretical.png"), "Theoretical");
        practicalButton = new ImageButton(
                ImageLibrary.load("resources/images/buttons/practical.png"), "Practical");

        theoreticalButton.addActionListener(e -> frame.showScreen(MainFrame.THEORETICAL));
        practicalButton.addActionListener(e -> frame.showScreen(MainFrame.PRACTICAL));

        closeButton = new JButton();
        closeButton.setOpaque(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setToolTipText("Back to menu");
        closeButton.addActionListener(e -> frame.showScreen(MainFrame.SPLASH));

        add(theoreticalButton);
        add(practicalButton);
        add(closeButton);
    }

    @Override
    public void doLayout() {
        double scale = Math.max(getWidth() / ART_W, getHeight() / ART_H);
        double offsetX = (getWidth() - ART_W * scale) / 2.0;
        double offsetY = (getHeight() - ART_H * scale) / 2.0;

        ImageButton[] buttons = {theoreticalButton, practicalButton};
        for (int i = 0; i < buttons.length; i++) {
            ImageButton button = buttons[i];
            int bw = (int) Math.round(BUTTON_WIDTH * scale);
            int bh = (int) Math.round(bw * button.aspectRatio());
            int padX = (int) Math.round(bw * PADDING);
            int padY = (int) Math.round(bh * PADDING);
            int cx = (int) Math.round(offsetX + BUTTON_CENTER_X * scale);
            int cy = (int) Math.round(offsetY + BUTTON_CENTER_Y[i] * scale);

            button.setBaseSize(bw, bh);
            button.setBounds(cx - bw / 2 - padX, cy - bh / 2 - padY, bw + 2 * padX, bh + 2 * padY);
        }

        closeButton.setBounds(
                (int) Math.round(offsetX + CLOSE_X * scale),
                (int) Math.round(offsetY + CLOSE_Y * scale),
                (int) Math.round(CLOSE_SIZE * scale),
                (int) Math.round(CLOSE_SIZE * scale));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        if (background != null) {
            double scale = Math.max((double) w / background.getWidth(), (double) h / background.getHeight());
            int drawW = (int) Math.round(background.getWidth() * scale);
            int drawH = (int) Math.round(background.getHeight() * scale);
            g2.drawImage(background, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
        } else {
            Theme.paintSky(g2, w, h);
        }
        g2.dispose();
    }
}
