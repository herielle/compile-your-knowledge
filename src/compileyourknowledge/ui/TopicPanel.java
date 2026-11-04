package compileyourknowledge.ui;

import compileyourknowledge.model.QuestionBank;
import compileyourknowledge.util.ImageLibrary;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Topic selection screen for one branch. Theoretical topics can be locked; Practical topics are always open. */
public class TopicPanel extends JPanel {
    private static final double ART_W = 2000;
    private static final double ART_H = 1414;
    private static final double PADDING = 0.16;

    private final MainFrame frame;
    private final String branch;
    private final boolean sequential;
    private final BufferedImage background = ImageLibrary.load("resources/images/background.png");
    private final List<ImageButton> buttons = new ArrayList<>();
    private final List<String> topics;
    private final JButton closeButton = new JButton();

    public TopicPanel(MainFrame frame, String branch, boolean sequential, QuestionBank bank) {
        this.frame = frame;
        this.branch = branch;
        this.sequential = sequential;
        this.topics = bank.topics(branch);
        setLayout(null);

        for (int i = 0; i < topics.size(); i++) {
            final int index = i;
            boolean unlocked = !sequential || frame.isTheoreticalUnlocked(i);
            ImageButton button = new ImageButton(ImageLibrary.load(imagePath(i, unlocked)), topics.get(i));
            button.addActionListener(e -> {
                if (isUnlocked(index)) frame.startTopic(branch, topics.get(index));
            });
            buttons.add(button);
            add(button);
        }

        closeButton.setOpaque(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setToolTipText("Back to program selection");
        closeButton.addActionListener(e -> frame.showScreen(MainFrame.START));
        add(closeButton);
        refreshLocks();
    }

    public void refreshLocks() {
        for (int i = 0; i < buttons.size(); i++) {
            ImageButton b = buttons.get(i);
            boolean unlocked = isUnlocked(i);
            b.setEnabled(unlocked);
            b.setImage(ImageLibrary.load(imagePath(i, unlocked)));
            b.setToolTipText(unlocked ? topics.get(i) : topics.get(i) + " (locked)");
        }
        repaint();
    }

    private boolean isUnlocked(int index) {
        return !sequential || frame.isTheoreticalUnlocked(index);
    }

    private String imagePath(int index, boolean unlocked) {
        if (branch.equalsIgnoreCase("Theoretical")) {
            int module = index + 1;
            return "resources/images/buttons/theoreticalButtons/module" + module
                    + (unlocked ? ".png" : "-locked.png");
        }
        String[] names = {"C.png", "java.png", "pyton.png", "haskell.png"};
        return "resources/images/buttons/practicalButtons/" + names[Math.min(index, names.length - 1)];
    }

    @Override
    public void doLayout() {
        double s = Math.max(getWidth() / ART_W, getHeight() / ART_H);
        double ox = (getWidth() - ART_W * s) / 2.0;
        double oy = (getHeight() - ART_H * s) / 2.0;
        int columns = branch.equalsIgnoreCase("Theoretical") ? 4 : 2;
        int rows = (int) Math.ceil(buttons.size() / (double) columns);
        double cellW = 360;
        double cellH = 300;
        double startX = (ART_W - columns * cellW) / 2.0;
        double startY = (ART_H - rows * cellH) / 2.0 + 40;

        for (int i = 0; i < buttons.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            ImageButton b = buttons.get(i);
            int bw = (int) Math.round(cellW * 0.78 * s);
            int bh = (int) Math.round(bw * b.aspectRatio());
            int padX = (int) Math.round(bw * PADDING);
            int padY = (int) Math.round(bh * PADDING);
            int cx = (int) Math.round(ox + (startX + col * cellW + cellW / 2) * s);
            int cy = (int) Math.round(oy + (startY + row * cellH + cellH / 2) * s);
            b.setBaseSize(bw, bh);
            b.setBounds(cx - bw / 2 - padX, cy - bh / 2 - padY, bw + 2 * padX, bh + 2 * padY);
        }
        closeButton.setBounds((int) Math.round(ox + 1824 * s), (int) Math.round(oy + 178 * s),
                (int) Math.round(54 * s), (int) Math.round(54 * s));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (background != null) {
            double s = Math.max((double) getWidth() / background.getWidth(), (double) getHeight() / background.getHeight());
            int w = (int) Math.round(background.getWidth() * s);
            int h = (int) Math.round(background.getHeight() * s);
            g2.drawImage(background, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
        } else Theme.paintSky(g2, getWidth(), getHeight());
        g2.dispose();
    }
}
