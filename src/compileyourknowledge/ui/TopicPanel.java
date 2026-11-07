package compileyourknowledge.ui;

import compileyourknowledge.model.QuestionBank;
import compileyourknowledge.util.Backdrop;
import compileyourknowledge.util.ImageLibrary;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

    /**
     * the topic picking screen for one branch, drawn as a retro window over the sky background
     *
     * theoretical: 8 modules in a 4 x 2 grid. a module shows a padlock until the one before it is cleared.
     * practical: the languages in one row, always open.
     *
     * it uses the same 2296 x 1624 artwork space as the question panel so both windows line up.
     */

public class TopicPanel extends JPanel {

    private static final double[] TITLE = {150, 235};
    private static final double[] CLOSE = {2095, 205, 62, 62};
    private static final double[] INNER = {106, 323, 2084, 1121};   // white body: x, y, w, h

    private static final int COLUMNS = 4;
    private static final double BUTTON_SIZE = 410;  
    private static final double PADDING = 0.14;    

    private final MainFrame frame;
    private final ArtSpace art = new ArtSpace(this);
    private final String branch;
    private final boolean sequential;
    private final BufferedImage background = ImageLibrary.load("resources/images/screens/background.png");
    private final List<String> topics;

    // every topic gets two buttons, made once. locking only switches which one is visible,
    // so nothing has to be reloaded when the grid is shown again

    private final List<ImageButton> openButtons = new ArrayList<>();
    private final List<ImageButton> lockedButtons = new ArrayList<>();
    private final JButton closeButton = new JButton();

    public TopicPanel(MainFrame frame, String branch, boolean sequential, QuestionBank bank) {
        this.frame = frame;
        this.branch = branch;
        this.sequential = sequential;
        this.topics = bank.topics(branch);
        setLayout(null);

        for (int i = 0; i < topics.size(); i++) {
            final int index = i;
            String topic = topics.get(i);

            ImageButton open = new ImageButton(ImageLibrary.load(imagePath(i, true)), topic);
            open.setToolTipText(topic);
            open.addActionListener(e -> {
                if (isUnlocked(index)) {
                    frame.startTopic(branch, topics.get(index));
                }
            });
            openButtons.add(open);
            add(open);

            // the first module is never locked, so it has no locked button
            ImageButton locked = null;
            if (sequential && i > 0) {
                locked = new ImageButton(ImageLibrary.load(imagePath(i, false)), topic + " (locked)");
                locked.setToolTipText("Locked: clear the previous module first");
                locked.setEnabled(false);
                add(locked);
            }
            lockedButtons.add(locked);
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

    // shows each topic as open or locked based on the player's progress
    public void refreshLocks() {
        for (int i = 0; i < topics.size(); i++) {
            boolean unlocked = isUnlocked(i);
            openButtons.get(i).setVisible(unlocked);
            ImageButton locked = lockedButtons.get(i);
            if (locked != null) {
                locked.setVisible(!unlocked);
            }
        }
        repaint();
    }

    // true if the topic at this position can be played. 
    private boolean isUnlocked(int index) {
        return !sequential || frame.isTheoreticalUnlocked(index);
    }

    // finds the button picture for a topic (open or locked version). 
    private String imagePath(int index, boolean unlocked) {
        if (branch.equalsIgnoreCase("Theoretical")) {
            int module = index + 1;
            return "resources/images/buttons/theoreticalButtons/module" + module
                    + (unlocked ? ".png" : "-locked.png");
        }
        String[] names = {"C.png", "java.png", "pyton.png", "haskell.png"};
        return "resources/images/buttons/practicalButtons/" + names[Math.min(index, names.length - 1)];
    }

    // lines the topic buttons up in a grid inside the white body of the window. 
    @Override
    public void doLayout() {
        closeButton.setBounds(art.sx(CLOSE[0]), art.sy(CLOSE[1]), art.sl(CLOSE[2]), art.sl(CLOSE[3]));

        int n = topics.size();
        if (n == 0) {
            return;
        }
        int rows = (n + COLUMNS - 1) / COLUMNS;
        double colWidth = INNER[2] / Math.min(n, COLUMNS);
        double rowHeight = INNER[3] / rows;
        for (int i = 0; i < n; i++) {
            double cx = INNER[0] + (i % COLUMNS + 0.5) * colWidth;
            double cy = INNER[1] + (i / COLUMNS + 0.5) * rowHeight;
            place(openButtons.get(i), cx, cy);
            if (lockedButtons.get(i) != null) {
                place(lockedButtons.get(i), cx, cy);
            }
        }
    }

    private void place(ImageButton button, double cx, double cy) {
        int bw = art.sl(BUTTON_SIZE);
        int bh = (int) Math.round(bw * button.aspectRatio());
        int padX = (int) Math.round(bw * PADDING);
        int padY = (int) Math.round(bh * PADDING);
        button.setBaseSize(bw, bh);
        button.setBounds(art.sx(cx) - bw / 2 - padX, art.sy(cy) - bh / 2 - padY, bw + 2 * padX, bh + 2 * padY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (background != null) {
            Backdrop.paint(g2, background, getWidth(), getHeight());
        } else {
            Theme.paintSky(g2, getWidth(), getHeight());
        }

        RetroPainter.paintEmptyWindow(g2, art);

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, art.sl(56))));
        g2.setColor(Color.WHITE);
        
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(branch, art.sx(TITLE[0]), art.sy(TITLE[1]) + (fm.getAscent() - fm.getDescent()) / 2);

        if (topics.isEmpty()) {
            paintEmptyMessage(g2);
        }
        g2.dispose();
    }

    // shows a message so the player doesn't just see an empty window when the question file is missing

    private void paintEmptyMessage(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, art.sl(44))));
        String msg = "No questions found for " + branch + ".";
        String hint = "Check resources/questions/questions.csv";
        
        FontMetrics fm = g2.getFontMetrics();
        int centerX = art.sx(INNER[0] + INNER[2] / 2);
        
        int y = art.sy(INNER[1] + INNER[3] / 2);
        g2.drawString(msg, centerX - fm.stringWidth(msg) / 2, y);
        g2.drawString(hint, centerX - fm.stringWidth(hint) / 2, y + fm.getHeight() + 8);
    }
}
