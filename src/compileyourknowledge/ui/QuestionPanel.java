package compileyourknowledge.ui;

import compileyourknowledge.model.Question;
import compileyourknowledge.util.Backdrop;
import compileyourknowledge.util.ImageLibrary;
import compileyourknowledge.util.ImageTrim;
import compileyourknowledge.util.SoundManager;

import javax.swing.JButton;
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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * The question window: it owns the round state (lives, score, lifelines) and wires the pieces
 * together. Drawing helpers live in {@link ArtSpace}, {@link RetroPainter}, {@link HeartsOverlay},
 * {@link AnswerMark} and {@link DialogOverlay}; saved games live in {@link SaveStore}.
 */
public class QuestionPanel extends JPanel {

    // ---- artwork-space layout (the mockup, 2296 x 1624 units) -------------------------
    private static final double[] TITLE = {150, 235};               // left edge, vertical center
    private static final double[] CLOSE = {2095, 205, 62, 62};      // the X in the title bar
    private static final double[] TEXT_BOX = {178, 378, 1950, 688}; // x, y, w, h of the scroll area (the whole grey panel)
    private static final double[] HEARTS = {1685, 392, 420, 120};   // x, y, w, h (up to 3 hearts, right aligned)

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

    private final MainFrame frame;
    private final ArtSpace art = new ArtSpace(this);
    private final SaveStore saveStore = SaveStore.inUserHome();

    private String windowTitle;
    /** Called at the start of every round, so each round can come back freshly shuffled. */
    private Supplier<Question[]> questionSource;
    /** Screen to return to when the window is closed (the topic grid the player came from). */
    private String backScreen = MainFrame.START;
    /** Called when a round is won; may unlock the next module and return a line to show, or null. */
    private Supplier<String> onWin;
    private Question[] questions = new Question[0];

    private final BufferedImage background = ImageLibrary.load("resources/images/screens/background.png");
    private final BufferedImage correctImage = ImageTrim.cropToAlpha(ImageLibrary.load("resources/images/correct.png"));
    private final BufferedImage wrongImage = ImageTrim.cropToAlpha(ImageLibrary.load("resources/images/wrong.png"));

    private final JTextArea textArea = new JTextArea();
    private final JScrollPane scroll;
    private final ImageButton[] letterButtons = new ImageButton[4];
    private final AnswerMark[] marks = new AnswerMark[4];
    private final HeartsOverlay heartsOverlay =
            new HeartsOverlay(ImageTrim.cropToAlpha(ImageLibrary.load("resources/images/heart.png")));
    private final ImageButton peekButton;
    private final ImageButton copyButton;
    private final ImageButton saveButton;
    private final JButton closeButton = new JButton();
    private final DialogOverlay overlay = new DialogOverlay(art);

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
            marks[i] = new AnswerMark();
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
        for (AnswerMark m : marks) {
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
        SavedGame saved = saveStore.read(windowTitle);
        if (saved == null) {
            return;
        }
        overlay.show(new String[]{"Continue your", "saved game?"}, null, saved.score,
                new String[]{"Yes", "No"},
                new Runnable[]{() -> { overlay.close(); applySave(saved); },
                               () -> { saveStore.clear(windowTitle); overlay.close(); refreshControls(); }});
    }

    private void applySave(SavedGame saved) {
        stopTimer();
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
        stopTimer(); // never let a pending "next question" fire after the window is closed
        overlay.close();
        if (frame != null) {
            frame.showScreen(backScreen);
        }
    }

    private void stopTimer() {
        if (nextTimer != null) {
            nextTimer.stop();
        }
    }

    // ------------------------------------------------------------------ layout

    @Override
    public void doLayout() {
        double s = art.scale();

        scroll.setBounds(art.sx(TEXT_BOX[0]), art.sy(TEXT_BOX[1]), art.sl(TEXT_BOX[2]), art.sl(TEXT_BOX[3]));
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.max(11, (int) Math.round(41 * s))));

        heartsOverlay.setBounds(art.sx(HEARTS[0]), art.sy(HEARTS[1]), art.sl(HEARTS[2]), art.sl(HEARTS[3]));
        // Leave room at the top of the text for the hearts; the text scrolls up past them.
        textArea.setMargin(new java.awt.Insets(art.sl(HEARTS[3] + 24), 10, 8, 10));

        closeButton.setBounds(art.sx(CLOSE[0]), art.sy(CLOSE[1]), art.sl(CLOSE[2]), art.sl(CLOSE[3]));

        for (int i = 0; i < 4; i++) {
            placeImageButton(letterButtons[i], LETTER_X[i], LETTER_Y, LETTER_SIZE, LETTER_SIZE, true);
            marks[i].setBounds(art.sx(LETTER_X[i]), art.sy(LETTER_Y), art.sl(LETTER_SIZE), art.sl(LETTER_SIZE));
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
        int bw = art.sl(aw);
        int bh = square ? art.sl(ah) : (int) Math.round(bw * button.aspectRatio());
        int cx = art.sx(ax + aw / 2);
        int cy = art.sy(ay + ah / 2);
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
            RetroPainter.paintWindow(g2, art);
        }

        // Title text in the artwork's title bar.
        Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, art.sl(56)));
        g2.setFont(titleFont);
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String title = finished ? windowTitle + " - " + (lives > 0 ? "You Win!" : "Game Over") : windowTitle;
        int ty = art.sy(TITLE[1]) + (fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(title, art.sx(TITLE[0]), ty);

        // Progress and score, small, along the bottom edge of the question panel.
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, art.sl(26))));
        g2.setColor(new Color(40, 40, 40));
        String status = "Question " + Math.min(current + 1, questions.length) + "/" + questions.length
                + "   Score: " + score;
        g2.drawString(status, art.sx(TEXT_BOX[0] + 14), art.sy(1108));
        g2.dispose();
    }

    // ------------------------------------------------------------------ round flow

    private void startRound() {
        stopTimer();
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
        Arrays.fill(removed, false);
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
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
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
        saveStore.clear(windowTitle);   // a finished round has nothing left to resume
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
            textArea.setText("GAME OVER\n\nYou ran out of hearts.\n\nScore: " + score
                    + "\nFinal score: " + score);
            textArea.setCaretPosition(0);
            overlay.show(new String[]{"GAME OVER!"}, null, score,
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
        SavedGame game = new SavedGame();
        game.score = score;
        game.current = current;
        game.lives = lives;
        game.peek = peekUsed;
        game.copy = copyUsed;
        game.questions = questions;
        saveStore.write(windowTitle, game);
        overlay.show(new String[]{"Current Score", "Saved!"}, null, -1,
                new String[]{"Back to Main Menu", "Exit"},
                new Runnable[]{this::backToMainMenu, () -> System.exit(0)});
    }

    private void backToMainMenu() {
        stopTimer();
        overlay.close();
        if (frame != null) {
            frame.showScreen(MainFrame.SPLASH);
        }
    }

    // ------------------------------------------------------------------ view helpers

    private void refreshControls() {
        heartsOverlay.setLives(lives);
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
