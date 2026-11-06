package compileyourknowledge.ui;

import compileyourknowledge.model.Question;
import compileyourknowledge.util.Backdrop;
import compileyourknowledge.util.ImageLibrary;
import compileyourknowledge.util.SoundManager;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

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
    // Peek and Copy can each be used once per game (independently of each other).
    // Save is not limited: it can be used any time the player still has hearts.
    private static final int MAX_LIVES = 3;
    private static final int POINTS_PER_LEVEL = 100;
    private static final int NEXT_QUESTION_DELAY_MS = 900;

    // Pop-up dialog (Save / Saved / Game Over / You Win), in artwork units.
    private static final double[] DLG = {588, 470, 1120, 660};   // x, y, w, h
    private static final double DLG_BTN_W = 440;
    private static final double DLG_BTN_H = 96;
    private static final double DLG_BTN_Y = 940;

    /** Saved checkpoint scores live here, one entry per topic, so they survive closing the game. */
    private static final File SAVE_FILE =
            new File(System.getProperty("user.home"), ".compileyourknowledge/save.properties");

    private final MainFrame frame;
    private String windowTitle;
    /** Called at the start of every round, so each round can come back freshly shuffled. */
    private Supplier<Question[]> questionSource;
    /** Screen to return to when the window is closed (the topic grid the player came from). */
    private String backScreen = MainFrame.START;
    /** Called when a round is won; may unlock the next module and return a line to show, or null. */
    private Supplier<String> onWin;
    private Question[] questions = new Question[0];

    private final BufferedImage background = ImageLibrary.load("resources/images/screens/background.png");
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
    private final DialogOverlay overlay = new DialogOverlay();

    // Round state.
    private int current;
    private int lives;
    private int score;
    private boolean peekUsed;
    private boolean copyUsed;
    private boolean locked;      // true while the correct mark shows, and after the round ends
    private boolean finished;
    private String note = "";    // short status line shown under the choices (hint, Copy, Save)
    private final boolean[] removed = new boolean[4];
    private Timer nextTimer;

    /** A panel that always plays the same fixed list of questions. */
    public QuestionPanel(MainFrame frame, String windowTitle, Question[] questions) {
        this(frame, windowTitle, () -> questions);
    }

    /** A panel that asks the supplier for a new set of questions at the start of every round. */
    public QuestionPanel(MainFrame frame, String windowTitle, Supplier<Question[]> questionSource) {
        this.frame = frame;
        this.windowTitle = windowTitle;
        this.questionSource = questionSource;
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

        // Add order is paint order top to bottom: hearts and marks sit above everything else.
        add(overlay);   // the pop-up dialog sits above everything and blocks clicks to what is below
        add(heartsOverlay);
        for (Mark m : marks) {
            add(m);
        }
        add(closeButton);
        for (ImageButton b : letterButtons) {
            add(b);
        }
        add(peekButton);
        add(copyButton);
        add(saveButton);
        add(scroll);

        startRound();
    }

    /**
     * Starts a fresh round for one topic. The one shared window is reused for every topic, so no new
     * panel (and no new copy of the artwork) is created per round.
     */
    public void startTopic(String windowTitle, Supplier<Question[]> questionSource,
                           String backScreen, Supplier<String> onWin) {
        this.windowTitle = windowTitle;
        this.questionSource = questionSource;
        this.backScreen = backScreen;
        this.onWin = onWin;
        startRound();
        offerResume();
    }

    /** If this topic has a saved game, asks whether to continue it; "No" throws the save away. */
    private void offerResume() {
        Saved saved = readSave();
        if (saved == null) {
            return;
        }
        overlay.show(new String[]{"Continue your", "saved game?"}, null, saved.score,
                new String[]{"Yes", "No"},
                new Runnable[]{() -> { overlay.close(); applySave(saved); },
                               () -> { clearSave(); overlay.close(); refreshControls(); }});
    }

    private void applySave(Saved saved) {
        if (nextTimer != null) {
            nextTimer.stop();
        }
        questions = saved.questions;
        current = saved.current;
        lives = saved.lives;
        score = saved.score;
        peekUsed = saved.peek;
        copyUsed = saved.copy;
        finished = false;
        showQuestion();
    }

    private void leave() {
        if (nextTimer != null) {
            nextTimer.stop(); // never let a pending "next question" fire after the window is closed
        }
        overlay.close();
        if (frame != null) {
            frame.showScreen(backScreen);
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

        overlay.setBounds(0, 0, getWidth(), getHeight());
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
            Backdrop.paint(g2, background, w, h);
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
        peekUsed = false;
        copyUsed = false;
        finished = false;
        overlay.close();
        questions = questionSource.get();
        if (questions == null || questions.length == 0) {
            finished = true;
            locked = true;
            lives = 0;
            questions = new Question[0];
            textArea.setText("No questions were found.\n\nCheck that resources/questions/questions.csv is in the project.");
            refreshControls();
            return;
        }
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
            sb.append((char) ('A' + i)).append(". ")
                    .append(q.getChoice(i).replace("\n", "\n   ")).append("\n");
        }
        if (!note.isEmpty()) {
            sb.append("\n").append(note);
        }
        textArea.setText(sb.toString());
        textArea.setCaretPosition(0);
        javax.swing.SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private void onAnswer(int choice) {
        if (locked || finished || removed[choice] || overlay.isVisible()) {
            return;
        }
        Question q = questions[current];
        if (choice == q.getCorrectIndex()) {
            SoundManager.playCorrect();
            score += POINTS_PER_LEVEL * q.getDifficulty();
            locked = true;
            marks[choice].setImage(correctImage);
            refreshControls();
            nextTimer = new Timer(NEXT_QUESTION_DELAY_MS, e -> advance());
            nextTimer.setRepeats(false);
            nextTimer.start();
        } else {
            SoundManager.playIncorrect();
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
        clearSave();   // a finished round has nothing left to resume
        refreshControls();
        if (won) {
            String unlocked = onWin == null ? null : onWin.get();
            textArea.setText("VICTORY!\n\nYou cleared every question in this topic.\n\nFinal score: " + score
                    + (unlocked == null ? "" : "\n\n" + unlocked));
            textArea.setCaretPosition(0);
            overlay.show(new String[]{"You Win!"}, unlocked, score,
                    new String[]{"Start Again", "Exit"},
                    new Runnable[]{this::startRound, () -> System.exit(0)});
        } else {
            int finalScore = score;
            textArea.setText("GAME OVER\n\nYou ran out of hearts.\n\nScore: " + score
                    + "\nFinal score: " + finalScore);
            textArea.setCaretPosition(0);
            overlay.show(new String[]{"GAME OVER!"}, null, finalScore,
                    new String[]{"Start Again", "Exit"},
                    new Runnable[]{this::startRound, () -> System.exit(0)});
        }
        repaint();
    }

    // ------------------------------------------------------------------ lifelines

    /** Peek and Copy: usable once each per game, and only while a question is open. */
    private boolean lifelineAvailable(boolean thisOneUsed) {
        return !(finished || locked || thisOneUsed || overlay.isVisible());
    }

    /** Save is never used up: it works any time the round is still going (the player still has hearts). */
    private boolean saveAvailable() {
        return !finished && !locked && lives > 0 && !overlay.isVisible();
    }

    private void usePeek() {
        if (!lifelineAvailable(peekUsed)) {
            return;
        }
        peekUsed = true;
        note = "Hint: " + questions[current].getHint();
        refreshText();
        refreshControls();
    }

    private void useCopy() {
        if (!lifelineAvailable(copyUsed)) {
            return;
        }
        copyUsed = true;
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

    /** Figure 2.3f: asks first; "Yes" banks the score (Figure 2.3g), "No" goes back to the question. */
    private void useSave() {
        if (!saveAvailable()) {
            return;
        }
        overlay.show(new String[]{"Do you want to save", "your Score?"}, null, score,
                new String[]{"Yes", "No"},
                new Runnable[]{this::confirmSave, () -> { overlay.close(); refreshControls(); }});
    }

    private void confirmSave() {
        writeSave();
        overlay.show(new String[]{"Current Score", "Saved!"}, null, -1,
                new String[]{"Back to Main Menu", "Exit"},
                new Runnable[]{this::backToMainMenu, () -> System.exit(0)});
    }

    private void backToMainMenu() {
        if (nextTimer != null) {
            nextTimer.stop();
        }
        overlay.close();
        if (frame != null) {
            frame.showScreen(MainFrame.SPLASH);
        }
    }

    // ------------------------------------------------------------------ saved game (on disk)

    /** Everything needed to carry on a round later, including the exact questions that were drawn. */
    private static final class Saved {
        int score;
        int current;
        int lives;
        boolean peek;
        boolean copy;
        Question[] questions;
    }

    private Properties loadProps() {
        Properties props = new Properties();
        if (SAVE_FILE.exists()) {
            try (FileInputStream in = new FileInputStream(SAVE_FILE)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Could not read save file: " + e);
            }
        }
        return props;
    }

    private void storeProps(Properties props) {
        try {
            File dir = SAVE_FILE.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(SAVE_FILE)) {
                props.store(out, "Compile Your Knowledge - saved games");
            }
        } catch (IOException e) {
            System.err.println("Could not write save file: " + e);
        }
    }

    private String saveKey(String name) {
        return windowTitle + "|" + name;
    }

    /** Saves the round as it stands at the start of the current question. */
    private void writeSave() {
        Properties props = loadProps();
        props.entrySet().removeIf(e -> String.valueOf(e.getKey()).startsWith(saveKey("")));
        props.setProperty(saveKey("score"), String.valueOf(score));
        props.setProperty(saveKey("current"), String.valueOf(current));
        props.setProperty(saveKey("lives"), String.valueOf(lives));
        props.setProperty(saveKey("peek"), String.valueOf(peekUsed));
        props.setProperty(saveKey("copy"), String.valueOf(copyUsed));
        props.setProperty(saveKey("count"), String.valueOf(questions.length));
        for (int i = 0; i < questions.length; i++) {
            Question q = questions[i];
            String k = "q" + i + ".";
            props.setProperty(saveKey(k + "topic"), q.getTopic() == null ? "" : q.getTopic());
            props.setProperty(saveKey(k + "difficulty"), String.valueOf(q.getDifficulty()));
            props.setProperty(saveKey(k + "prompt"), q.getPrompt());
            for (int c = 0; c < 4; c++) {
                props.setProperty(saveKey(k + "choice" + c), q.getChoice(c));
            }
            props.setProperty(saveKey(k + "correct"), String.valueOf(q.getCorrectIndex()));
            props.setProperty(saveKey(k + "hint"), q.getHint() == null ? "" : q.getHint());
        }
        storeProps(props);
    }

    /** The saved game for the current topic, or null if there is none (or it cannot be read). */
    private Saved readSave() {
        Properties props = loadProps();
        if (props.getProperty(saveKey("count")) == null) {
            return null;
        }
        try {
            Saved saved = new Saved();
            saved.score = Integer.parseInt(props.getProperty(saveKey("score")));
            saved.current = Integer.parseInt(props.getProperty(saveKey("current")));
            saved.lives = Integer.parseInt(props.getProperty(saveKey("lives")));
            saved.peek = Boolean.parseBoolean(props.getProperty(saveKey("peek")));
            saved.copy = Boolean.parseBoolean(props.getProperty(saveKey("copy")));
            int count = Integer.parseInt(props.getProperty(saveKey("count")));
            saved.questions = new Question[count];
            for (int i = 0; i < count; i++) {
                String k = "q" + i + ".";
                String[] choices = new String[4];
                for (int c = 0; c < 4; c++) {
                    choices[c] = props.getProperty(saveKey(k + "choice" + c));
                }
                saved.questions[i] = new Question(
                        props.getProperty(saveKey(k + "topic")),
                        Integer.parseInt(props.getProperty(saveKey(k + "difficulty"))),
                        props.getProperty(saveKey(k + "prompt")),
                        choices,
                        Integer.parseInt(props.getProperty(saveKey(k + "correct"))),
                        props.getProperty(saveKey(k + "hint")));
            }
            if (count == 0 || saved.current < 0 || saved.current >= count || saved.lives <= 0) {
                return null;
            }
            return saved;
        } catch (RuntimeException e) {   // missing key, bad number, null choice: treat as no save
            return null;
        }
    }

    private void clearSave() {
        Properties props = loadProps();
        if (props.entrySet().removeIf(e -> String.valueOf(e.getKey()).startsWith(saveKey("")))) {
            storeProps(props);
        }
    }

    // ------------------------------------------------------------------ pop-up dialog

    /**
     * The retro pop-up from the mockups: a dimmed window behind a grey dialog with a blue title bar,
     * one or two message lines, an optional SCORE box and two buttons. It covers the whole panel so
     * nothing underneath can be clicked while it is open.
     */
    private class DialogOverlay extends JComponent {
        private String[] lines = {};
        private String extra;
        private int shownScore = -1;     // -1 hides the SCORE box
        private String[] labels = {};
        private Runnable[] actions = {};
        private int hover = -1;

        DialogOverlay() {
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
                        SoundManager.playClick(); // the dialog buttons are painted by hand, not real buttons
                        actions[h].run();
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

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

        void close() {
            setVisible(false);
            hover = -1;
            QuestionPanel.this.repaint();
        }

        private void setHover(int h) {
            if (h != hover) {
                hover = h;
                repaint();
            }
        }

        private int[] buttonRect(int i) {
            double cx = DLG[0] + DLG[2] * (2 * i + 1) / (2.0 * labels.length);
            return new int[]{sx(cx - DLG_BTN_W / 2), sy(DLG_BTN_Y), sl(DLG_BTN_W), sl(DLG_BTN_H)};
        }

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

            Color grey = new Color(192, 192, 192);
            boxArt(g2, DLG[0], DLG[1], DLG[2], DLG[3], 6, grey);
            fillArt(g2, DLG[0] + 6, DLG[1] + 6, DLG[2] - 12, 110, new Color(0, 0, 205));   // blue bar
            fillArt(g2, DLG[0] + 6, DLG[1] + 116, DLG[2] - 12, 30, new Color(224, 224, 224));

            // Message block, centred in the space between the title bar and the buttons.
            double lineH = 88;
            double extraH = extra == null ? 0 : 64;
            double scoreH = shownScore < 0 ? 0 : 110;
            double block = lines.length * lineH + extraH + scoreH;
            double y = 620 + (320 - block) / 2;
            double cx = DLG[0] + DLG[2] / 2;

            g2.setColor(Color.BLACK);
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(14, sl(lines.length == 1 ? 80 : 62))));
            for (String line : lines) {
                drawCentered(g2, line, cx, y + lineH * 0.7);
                y += lineH;
            }
            if (extra != null) {
                g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, sl(36))));
                drawCentered(g2, extra, cx, y + 40);
                y += extraH;
            }
            if (shownScore >= 0) {
                y += 20;
                boxArt(g2, cx - 190, y, 380, 66, 4, Color.WHITE);
                g2.setColor(Color.BLACK);
                g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, sl(36))));
                drawCentered(g2, "SCORE : " + shownScore, cx, y + 46);
            }

            // Buttons.
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, sl(40))));
            for (int i = 0; i < labels.length; i++) {
                int[] r = buttonRect(i);
                int b = Math.max(2, sl(5));
                g2.setColor(Color.BLACK);
                g2.fillRect(r[0], r[1], r[2], r[3]);
                g2.setColor(i == hover ? new Color(225, 225, 225) : new Color(205, 205, 205));
                g2.fillRect(r[0] + b, r[1] + b, r[2] - 2 * b, r[3] - 2 * b);
                g2.setColor(Color.WHITE);                                       // bevel highlight
                g2.fillRect(r[0] + b, r[1] + b, r[2] - 2 * b, Math.max(1, b / 2));
                g2.setColor(Color.BLACK);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(labels[i], r[0] + (r[2] - fm.stringWidth(labels[i])) / 2,
                        r[1] + (r[3] + fm.getAscent() - fm.getDescent()) / 2);
            }
            g2.dispose();
        }

        private void drawCentered(Graphics2D g2, String text, double artCx, double artBaseline) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, sx(artCx) - fm.stringWidth(text) / 2, sy(artBaseline));
        }
    }

    // ------------------------------------------------------------------ view helpers

    private void refreshControls() {
        for (int i = 0; i < 4; i++) {
            letterButtons[i].setEnabled(!locked && !finished && !removed[i]);
            if (!removed[i] && !(locked && !finished && i == questions[current].getCorrectIndex())) {
                marks[i].setImage(null);
            }
        }
        peekButton.setEnabled(lifelineAvailable(peekUsed));
        copyButton.setEnabled(lifelineAvailable(copyUsed));
        saveButton.setEnabled(saveAvailable());
        repaint();
    }
}