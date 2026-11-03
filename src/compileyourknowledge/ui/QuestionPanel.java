package compileyourknowledge.ui;

import compileyourknowledge.model.Question;
import compileyourknowledge.util.ImageLibrary;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionPanel extends JPanel {

    // ---- artwork-space layout (the mockup, 2296 x 1624 units) -------------------------
    private static final double ART_W = 2296;
    private static final double ART_H = 1624;

    private static final double[] TITLE = {150, 235};               // left edge, vertical center
    private static final double[] CLOSE = {2095, 205, 62, 62};      // the X in the title bar
    private static final double[] TEXT_BOX = {178, 378, 1950, 688}; // x, y, w, h of the scroll area (the whole grey panel)
    private static final double[] HEARTS = {1685, 392, 420, 120};    // x, y, w, h (up to 3 hearts, right aligned)

    private static final double LETTER_Y = 1162;
    private static final double LETTER_SIZE = 234;
    private static final double[] LETTER_X = {160, 452, 743, 1028};

    // Lifeline buttons: x, y, w, h  (Peek, Copy, Save)
    private static final double[][] LIFELINE = {
            {1313, 1294, 375, 102},
            {1536, 1168, 377, 102},
            {1756, 1294, 375, 102}};

    
    private static final boolean DRAW_WINDOW = true;

    private static final double PADDING = 0.12; // room around image buttons for the hover grow

    // ---- game rules ---------------------------------------------------------------
    /** The proposal allows one lifeline per game. Set to false to allow each one once. */
    private static final boolean ONE_LIFELINE_PER_GAME = true;
    private static final int MAX_LIVES = 3;
    private static final int POINTS_PER_LEVEL = 100;
    private static final int NEXT_QUESTION_DELAY_MS = 900;

    private final MainFrame frame;
    private final String windowTitle;
    private final Question[] questions;

    private final BufferedImage background = ImageLibrary.load("resources/images/background.png");
    private final BufferedImage heartImage = cropToAlpha(ImageLibrary.load("resources/images/heart.png"));
    private final BufferedImage correctImage = cropToAlpha(ImageLibrary.load("resources/images/correct.png"));
    private final BufferedImage wrongImage = cropToAlpha(ImageLibrary.load("resources/images/wrong.png"));

    private final JTextArea textArea = new JTextArea();
    private final JScrollPane scroll;
    private final ImageButton[] letterButtons = new ImageButton[4];
    private final Mark[] marks = new Mark[4];
    private final HeartsOverlay heartsOverlay = new HeartsOverlay();
    private final ImageButton peekButton;
    private final ImageButton copyButton;
    private final ImageButton saveButton;
    private final JButton closeButton = new JButton();
    private final JButton againButton = new JButton("Start Again");

    // Round state.
    private int current;
    private int lives;
    private int score;
    private int savedScore;      // -1 means nothing saved yet
    private boolean lifelineUsed;
    private boolean peekUsed;
    private boolean copyUsed;
    private boolean saveUsed;
    private boolean locked;      // true while the correct mark shows, and after the round ends
    private boolean finished;
    private String note = "";    // short status line shown under the choices (hint, Copy, Save)
    private final boolean[] removed = new boolean[4];
    private Timer nextTimer;

    public QuestionPanel(MainFrame frame, String windowTitle, Question[] questions) {
        this.frame = frame;
        this.windowTitle = windowTitle;
        this.questions = questions;
        setLayout(null); // positioned by doLayout()

        // Scrollable text inside the grey question panel.
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setForeground(Color.BLACK);
        textArea.setMargin(new java.awt.Insets(6, 8, 6, 8));

        scroll = new JScrollPane(textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        // No visible scroll bar; the mouse wheel and trackpad still scroll the text.
        scroll.getVerticalScrollBar().setPreferredSize(new java.awt.Dimension(0, 0));

        String[] letters = {"a", "b", "c", "d"};
        for (int i = 0; i < 4; i++) {
            final int choice = i;
            letterButtons[i] = new ImageButton(
                    ImageLibrary.load("resources/images/buttons/" + letters[i] + "_letter.png"),
                    String.valueOf((char) ('A' + i)));
            letterButtons[i].addActionListener(e -> onAnswer(choice));
            marks[i] = new Mark();
        }
        peekButton = new ImageButton(ImageLibrary.load("resources/images/buttons/peek_button.png"), "Peek");
        copyButton = new ImageButton(ImageLibrary.load("resources/images/buttons/copy_button.png"), "Copy");
        saveButton = new ImageButton(ImageLibrary.load("resources/images/buttons/save_button.png"), "Save");
        peekButton.addActionListener(e -> usePeek());
        copyButton.addActionListener(e -> useCopy());
        saveButton.addActionListener(e -> useSave());

        // Invisible button over the X in the artwork's title bar.
        closeButton.setOpaque(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setToolTipText("Back to menu");
        closeButton.addActionListener(e -> leave());

        againButton.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        againButton.addActionListener(e -> startRound());
        againButton.setVisible(false);

        // Add order is paint order top to bottom: hearts and marks sit above everything else.
        add(heartsOverlay);
        for (Mark m : marks) {
            add(m);
        }
        add(closeButton);
        add(againButton);
        for (ImageButton b : letterButtons) {
            add(b);
        }
        add(peekButton);
        add(copyButton);
        add(saveButton);
        add(scroll);

        startRound();
    }

    private void leave() {
        startRound(); // reset so the next visit starts fresh
        if (frame != null) {
            frame.showScreen(MainFrame.START);
        }
    }

    // ------------------------------------------------------------------ layout

    private double scale() {
        return Math.max(getWidth() / ART_W, getHeight() / ART_H);
    }

    private double offsetX() {
        return (getWidth() - ART_W * scale()) / 2.0;
    }

    private double offsetY() {
        return (getHeight() - ART_H * scale()) / 2.0;
    }

    private int sx(double artX) {
        return (int) Math.round(offsetX() + artX * scale());
    }

    private int sy(double artY) {
        return (int) Math.round(offsetY() + artY * scale());
    }

    private int sl(double artLength) {
        return (int) Math.round(artLength * scale());
    }

    @Override
    public void doLayout() {
        double s = scale();

        scroll.setBounds(sx(TEXT_BOX[0]), sy(TEXT_BOX[1]), sl(TEXT_BOX[2]), sl(TEXT_BOX[3]));
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.max(11, (int) Math.round(41 * s))));

        heartsOverlay.setBounds(sx(HEARTS[0]), sy(HEARTS[1]), sl(HEARTS[2]), sl(HEARTS[3]));
        // Leave room at the top of the text for the hearts; the text scrolls up past them.
        textArea.setMargin(new java.awt.Insets(sl(HEARTS[3] + 24), 10, 8, 10));

        closeButton.setBounds(sx(CLOSE[0]), sy(CLOSE[1]), sl(CLOSE[2]), sl(CLOSE[3]));

        for (int i = 0; i < 4; i++) {
            placeImageButton(letterButtons[i], LETTER_X[i], LETTER_Y, LETTER_SIZE, LETTER_SIZE, true);
            marks[i].setBounds(sx(LETTER_X[i]), sy(LETTER_Y), sl(LETTER_SIZE), sl(LETTER_SIZE));
        }
        ImageButton[] lifelines = {peekButton, copyButton, saveButton};
        for (int i = 0; i < 3; i++) {
            double[] r = LIFELINE[i];
            placeImageButton(lifelines[i], r[0], r[1], r[2], r[3], false);
        }

        int aw = sl(420);
        int ah = sl(90);
        againButton.setBounds(sx(ART_W / 2) - aw / 2, sy(1010) - ah / 2, aw, ah);
    }

    /** Places a button so its visible image fills the given artwork rectangle. */
    private void placeImageButton(ImageButton button, double ax, double ay, double aw, double ah, boolean square) {
        int bw = sl(aw);
        int bh = square ? sl(ah) : (int) Math.round(bw * button.aspectRatio());
        int cx = sx(ax + aw / 2);
        int cy = sy(ay + ah / 2);
        int padX = (int) Math.round(bw * PADDING);
        int padY = (int) Math.round(bh * PADDING);
        button.setBaseSize(bw, bh);
        button.setBounds(cx - bw / 2 - padX, cy - bh / 2 - padY, bw + 2 * padX, bh + 2 * padY);
    }

    // ------------------------------------------------------------------ painting

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
            double s = Math.max((double) w / background.getWidth(), (double) h / background.getHeight());
            int dw = (int) Math.round(background.getWidth() * s);
            int dh = (int) Math.round(background.getHeight() * s);
            g2.drawImage(background, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        } else {
            Theme.paintSky(g2, w, h);
        }

        if (DRAW_WINDOW) {
            paintWindow(g2);
        }

        // Title text in the artwork's title bar.
        Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, sl(56)));
        g2.setFont(titleFont);
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String title = finished ? windowTitle + " - " + (lives > 0 ? "You Win!" : "Game Over") : windowTitle;
        int ty = sy(TITLE[1]) + (fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(title, sx(TITLE[0]), ty);

        // Progress and score, small, along the bottom edge of the question panel.
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, sl(26))));
        g2.setColor(new Color(40, 40, 40));
        String status = "Question " + Math.min(current + 1, questions.length) + "/" + questions.length
                + "   Score: " + score;
        g2.drawString(status, sx(TEXT_BOX[0] + 14), sy(1108));
        g2.dispose();
    }

    /** Draws one heart per remaining life, right aligned, on top of the scrolling text. */
    private class HeartsOverlay extends JComponent {
        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int h = getHeight();
            int w = heartImage == null ? h
                    : (int) Math.round(h * (double) heartImage.getWidth() / heartImage.getHeight());
            int gap = Math.max(2, h / 24);
            for (int i = 0; i < lives; i++) {
                int x = getWidth() - (i + 1) * w - i * gap;
                if (heartImage != null) {
                    g2.drawImage(heartImage, x, 0, w, h, null);
                } else {
                    g2.setColor(Color.RED);
                    g2.fillOval(x, 0, h, h);
                }
            }
            g2.dispose();
        }
    }

    /** Trims transparent borders so the picture fills the box it is drawn into. */
    private static BufferedImage cropToAlpha(BufferedImage img) {
        if (img == null) {
            return null;
        }
        int minX = img.getWidth();
        int minY = img.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (((img.getRGB(x, y) >>> 24) & 0xFF) > 10) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return img;
        }
        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private void fillArt(Graphics2D g2, double x, double y, double w, double h, Color c) {
        g2.setColor(c);
        g2.fillRect(sx(x), sy(y), sl(w), sl(h));
    }

    /** A black-bordered box: the border is drawn first, then the fill inset by the border width. */
    private void boxArt(Graphics2D g2, double x, double y, double w, double h, double border, Color fill) {
        fillArt(g2, x, y, w, h, Color.BLACK);
        fillArt(g2, x + border, y + border, w - 2 * border, h - 2 * border, fill);
    }

    /** The retro window from the mockup, drawn in artwork coordinates. */
    private void paintWindow(Graphics2D g2) {
        Color grey = new Color(192, 192, 192);
        boxArt(g2, 80, 145, 2136, 1325, 6, grey);                        // outer frame
        fillArt(g2, 108, 175, 2080, 122, new Color(18, 0, 120));          // title bar
        boxArt(g2, 106, 323, 2084, 1121, 6, Color.WHITE);                 // white inner area
        boxArt(g2, 163, 360, 1970, 772, 12, grey);                        // question panel
        fillArt(g2, 175, 372, 1946, 12, new Color(222, 222, 222));        // top highlight

        // Minimize, maximize, close boxes.
        double[] bx = {1907, 2000, 2095};
        for (double x : bx) {
            fillArt(g2, x, 205, 62, 62, grey);
        }
        fillArt(g2, 1925, 245, 28, 8, Color.BLACK);                        // minimize bar
        boxArt(g2, 2018, 222, 26, 26, 6, grey);                           // maximize box
        g2.setColor(Color.BLACK);
        g2.setStroke(new java.awt.BasicStroke(Math.max(2f, (float) (7 * scale()))));
        g2.drawLine(sx(2112), sy(222), sx(2142), sy(250));                // close X
        g2.drawLine(sx(2142), sy(222), sx(2112), sy(250));
    }

    /** A small overlay that draws the correct check or the wrong X on top of a letter button. */
    private static class Mark extends JComponent {
        private BufferedImage image;

        void setImage(BufferedImage img) {
            if (img != image) {
                image = img;
                repaint();
            }
        }

        @Override
        public boolean contains(int x, int y) {
            return false; // clicks pass straight through to the letter button below
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (image == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ round flow

    private void startRound() {
        if (nextTimer != null) {
            nextTimer.stop();
        }
        current = 0;
        lives = MAX_LIVES;
        score = 0;
        savedScore = -1;
        lifelineUsed = false;
        peekUsed = false;
        copyUsed = false;
        saveUsed = false;
        finished = false;
        againButton.setVisible(false);
        showQuestion();
    }

    private void showQuestion() {
        java.util.Arrays.fill(removed, false);
        locked = false;
        note = "";
        refreshText();
        refreshControls();
    }

    /** Question, then the four choice lines, then any hint or lifeline note. */
    private void refreshText() {
        if (finished) {
            return;
        }
        Question q = questions[current];
        StringBuilder sb = new StringBuilder(q.getPrompt()).append("\n\n");
        for (int i = 0; i < 4; i++) {
            sb.append((char) ('A' + i)).append(". ").append(q.getChoice(i)).append("\n");
        }
        if (!note.isEmpty()) {
            sb.append("\n").append(note);
        }
        textArea.setText(sb.toString());
        textArea.setCaretPosition(0);
        javax.swing.SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private void onAnswer(int choice) {
        if (locked || finished || removed[choice]) {
            return;
        }
        Question q = questions[current];
        if (choice == q.getCorrectIndex()) {
            score += POINTS_PER_LEVEL * q.getDifficulty();
            locked = true;
            marks[choice].setImage(correctImage);
            refreshControls();
            nextTimer = new Timer(NEXT_QUESTION_DELAY_MS, e -> advance());
            nextTimer.setRepeats(false);
            nextTimer.start();
        } else {
            removed[choice] = true;
            lives--;
            marks[choice].setImage(wrongImage);
            if (lives <= 0) {
                endRound(false);
            } else {
                refreshControls();
            }
        }
    }

    private void advance() {
        if (current + 1 >= questions.length) {
            endRound(true);
        } else {
            current++;
            showQuestion();
        }
    }

    private void endRound(boolean won) {
        finished = true;
        locked = true;
        String result;
        if (won) {
            result = "VICTORY!\n\nYou cleared every question in this topic.\n\nFinal score: " + score;
        } else {
            result = "GAME OVER\n\nYou ran out of hearts.\n\nScore: " + score
                    + (savedScore >= 0 ? "\nSaved checkpoint: " + savedScore : "")
                    + "\nFinal score: " + Math.max(score, savedScore);
        }
        textArea.setText(result);
        textArea.setCaretPosition(0);
        againButton.setVisible(true);
        refreshControls();
    }

    // ------------------------------------------------------------------ lifelines

    private boolean lifelineAvailable(boolean thisOneUsed) {
        if (finished || locked || thisOneUsed) {
            return false;
        }
        return !(ONE_LIFELINE_PER_GAME && lifelineUsed);
    }

    private void usePeek() {
        if (!lifelineAvailable(peekUsed)) {
            return;
        }
        peekUsed = true;
        lifelineUsed = true;
        note = "Hint: " + questions[current].getHint();
        refreshText();
        refreshControls();
    }

    private void useCopy() {
        if (!lifelineAvailable(copyUsed)) {
            return;
        }
        copyUsed = true;
        lifelineUsed = true;
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i != questions[current].getCorrectIndex() && !removed[i]) {
                wrong.add(i);
            }
        }
        Collections.shuffle(wrong);
        for (int k = 0; k < Math.min(2, wrong.size()); k++) {
            removed[wrong.get(k)] = true;
            marks[wrong.get(k)].setImage(wrongImage);
        }
        refreshControls();
    }

    private void useSave() {
        if (!lifelineAvailable(saveUsed)) {
            return;
        }
        saveUsed = true;
        lifelineUsed = true;
        savedScore = score;
        note = "Score of " + score + " saved as a checkpoint.";
        refreshText();
        refreshControls();
    }

    // ------------------------------------------------------------------ view helpers

    private void refreshControls() {
        for (int i = 0; i < 4; i++) {
            letterButtons[i].setEnabled(!locked && !finished && !removed[i]);
            if (!removed[i] && !(locked && i == questions[current].getCorrectIndex() && !finished)) {
                marks[i].setImage(null);
            }
        }
        peekButton.setEnabled(lifelineAvailable(peekUsed));
        copyButton.setEnabled(lifelineAvailable(copyUsed));
        saveButton.setEnabled(lifelineAvailable(saveUsed));
        repaint();
    }
}