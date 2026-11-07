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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * the question window. it keeps track of the round (lives, score, lifelines) and connects the other
 * pieces together. the drawing helpers are in ArtSpace, RetroPainter, HeartsOverlay, AnswerMark and
 * DialogOverlay. saved games are handled by SaveStore.
 */

public class QuestionPanel extends JPanel {

    // positions in the artwork (2296 x 1624 units)
    private static final double[] TITLE = {150, 235};               // left edge and vertical middle
    private static final double[] CLOSE = {2095, 205, 62, 62};      // the x in the title bar
    private static final double[] TEXT_BOX = {178, 378, 1950, 688}; // x, y, w, h of the scroll area (the whole grey panel)
    private static final double[] HEARTS = {1685, 392, 420, 120};   // x, y, w, h (up to 3 hearts, right aligned)

    private static final double LETTER_Y = 1162;
    private static final double LETTER_SIZE = 234;
    private static final double[] LETTER_X = {160, 452, 743, 1028};

    // lifeline buttons (peek, copy, save): x, y, w, h
    private static final double[][] LIFELINE = {
            {1313, 1294, 375, 102},
            {1536, 1168, 377, 102},
            {1756, 1294, 375, 102}};

    private static final double PADDING = 0.12; // extra room around image buttons so they can grow on hover

    // game rules:
    // peek and copy can each be used once per game.
    // save has no limit, as long as the player still has hearts.
    // hearts at the start of a new topic. moving to the next module keeps whatever is left.
    public static final int MAX_LIVES = 3;
    private static final int POINTS_PER_LEVEL = 100;
    private static final int NEXT_QUESTION_DELAY_MS = 900;

    private final MainFrame frame;
    private final ArtSpace art = new ArtSpace(this);
    private final SaveStore saveStore = SaveStore.inUserHome();

    private String windowTitle;
    private Supplier<Question[]> questionSource;   // called at the start of every round for a fresh shuffled set
    private String backScreen = MainFrame.START;   // screen to go back to when the window is closed
    private Supplier<String> onWin;                // called on a win. may unlock the next module and return a line to show (or null)
    private BiConsumer<Integer, Integer> onNextLevel; // starts the next module with the hearts left and the score so far. null if there is no next module
    private Runnable onRestart;                    // called on game over + start again. goes back to the first module. null just restarts this topic
    private Runnable onLose;                       // called the moment the player runs out of hearts. resets the module progress. null if nothing to reset
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

    // round state
    private int current;
    private int lives;
    private int score;
    private boolean peekUsed;
    private boolean copyUsed;
    private boolean locked;      // true while the correct mark shows, and after the round ends
    private boolean finished;
    private String note = "";    // short line under the choices (hint, copy, save)
    private final boolean[] removed = new boolean[4];
    private Timer nextTimer;
    private int textFontSize = -1;     // last values given to the text area, so layout only changes them when needed
    private int textTopMargin = -1;

    // a panel that always plays the same fixed list of questions.
    public QuestionPanel(MainFrame frame, String windowTitle, Question[] questions) {
        this(frame, windowTitle, () -> questions);
    }

    // a panel that asks the supplier for a new set of questions at the start of every round.
    public QuestionPanel(MainFrame frame, String windowTitle, Supplier<Question[]> questionSource) {
        this.frame = frame;
        this.windowTitle = windowTitle;
        this.questionSource = questionSource;
        setLayout(null); // doLayout() places everything

        // scrollable text inside the grey question panel
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setForeground(Color.BLACK);
        textArea.setMargin(new Insets(6, 8, 6, 8));

        scroll = new JScrollPane(textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        // the scroll bar is hidden, but the mouse wheel and trackpad still scroll the text
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

        String[] letters = {"a", "b", "c", "d"};
        for (int i = 0; i < 4; i++) {
            final int choice = i;
            letterButtons[i] = new ImageButton(
                    ImageLibrary.load("resources/images/buttons/" + letters[i] + "_letter.png"),
                    String.valueOf((char) ('A' + i)));
            letterButtons[i].setDimWhenDisabled(true);
            letterButtons[i].addActionListener(e -> onAnswer(choice));
            marks[i] = new AnswerMark();
        }
        peekButton = new ImageButton(ImageLibrary.load("resources/images/buttons/peek_button.png"), "Peek");
        copyButton = new ImageButton(ImageLibrary.load("resources/images/buttons/copy_button.png"), "Copy");
        saveButton = new ImageButton(ImageLibrary.load("resources/images/buttons/save_button.png"), "Save");
        for (ImageButton lifeline : new ImageButton[]{peekButton, copyButton, saveButton}) {
            lifeline.setDimWhenDisabled(true); // a used lifeline looks used
        }
        peekButton.addActionListener(e -> usePeek());
        copyButton.addActionListener(e -> useCopy());
        saveButton.addActionListener(e -> useSave());

        // invisible button over the x in the title bar
        closeButton.setOpaque(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setToolTipText("Back to menu");
        closeButton.addActionListener(e -> leave());

        // the order things are added is the paint order, top to bottom.
        // the dialog goes first so it sits above everything and blocks clicks to what's below
        add(overlay);
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
     * starts a fresh round for one topic. the same window is reused for every topic,
     * so no new panel (or new copy of the artwork) is made each round.
     */
    public void startTopic(String windowTitle, Supplier<Question[]> questionSource,
                           String backScreen, Supplier<String> onWin,
                           BiConsumer<Integer, Integer> onNextLevel, Runnable onRestart, Runnable onLose,
                           int startLives, int startScore, boolean resumeNow) {
        this.windowTitle = windowTitle;
        this.questionSource = questionSource;
        this.backScreen = backScreen;
        this.onWin = onWin;
        this.onNextLevel = onNextLevel;
        this.onRestart = onRestart;
        this.onLose = onLose;
        startRound(startLives, startScore);
        SavedGame saved = resumeNow ? saveStore.read(windowTitle) : null;
        if (saved != null) {
            applySave(saved);   // the player already said yes on the topic screen, so no second question
        } else {
            offerResume();
        }
    }

    // if this topic has a saved game, asks if the player wants to continue it. "no" deletes the save.
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

    // loads a saved game back into the round.
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

        // put back what had already happened on this question
        System.arraycopy(saved.removed, 0, removed, 0, 4);
        if (saved.hintShown) {
            note = "Hint: " + questions[current].getHint();
        }
        for (int i = 0; i < 4; i++) {
            if (removed[i]) {
                marks[i].setImage(wrongImage);
            }
        }
        refreshText();
        refreshControls();
    }

    // closes the question window and goes back to the topic screen.
    private void leave() {
        stopTimer(); // so a waiting "next question" can't fire after the window is closed
        overlay.close();
        if (frame != null) {
            frame.showScreen(backScreen);
        }
    }

    // stops the timer that moves to the next question.
    private void stopTimer() {
        if (nextTimer != null) {
            nextTimer.stop();
        }
    }

    // places the text, hearts, close spot and buttons for the current window size.
    @Override
    public void doLayout() {
        double s = art.scale();

        scroll.setBounds(art.sx(TEXT_BOX[0]), art.sy(TEXT_BOX[1]), art.sl(TEXT_BOX[2]), art.sl(TEXT_BOX[3]));
        int fontSize = Math.max(11, (int) Math.round(41 * s));
        if (fontSize != textFontSize) {
            textFontSize = fontSize;
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
        }

        heartsOverlay.setBounds(art.sx(HEARTS[0]), art.sy(HEARTS[1]), art.sl(HEARTS[2]), art.sl(HEARTS[3]));
        // leave space at the top of the text for the hearts. the text scrolls up past them
        int topMargin = art.sl(HEARTS[3] + 24);
        if (topMargin != textTopMargin) {
            textTopMargin = topMargin;
            textArea.setMargin(new Insets(topMargin, 10, 8, 10));
        }

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

    // places a button so its visible picture fills the given artwork rectangle.
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

    // draws the background, the window, the title and the question/score line.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (background != null) {
            Backdrop.paint(g2, background, getWidth(), getHeight());
        } else {
            Theme.paintSky(g2, getWidth(), getHeight());
        }

        RetroPainter.paintWindow(g2, art);

        // title text in the title bar
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, art.sl(56))));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String title = finished ? windowTitle + " - " + (lives > 0 ? "You Win!" : "Game Over") : windowTitle;
        int titleY = art.sy(TITLE[1]) + (fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(title, art.sx(TITLE[0]), titleY);

        // small progress and score line along the bottom of the question panel
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, art.sl(26))));
        g2.setColor(new Color(40, 40, 40));
        String status = "Question " + Math.min(current + 1, questions.length) + "/" + questions.length
                + "   Score: " + score;
        g2.drawString(status, art.sx(TEXT_BOX[0] + 14), art.sy(1108));
        g2.dispose();
    }

    // starts a round with full hearts and a score of 0.
    private void startRound() {
        startRound(MAX_LIVES, 0);
    }

    // starts a round with this many hearts and this score (both carried over from the last module).
    private void startRound(int startLives, int startScore) {
        stopTimer();
        current = 0;
        lives = Math.max(1, Math.min(MAX_LIVES, startLives));
        score = startScore;
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

    // shows the current question with a clean slate (nothing crossed out, no note).
    private void showQuestion() {
        Arrays.fill(removed, false);
        locked = false;
        note = "";
        refreshText();
        refreshControls();
    }

    // fills the text area: the question, the four choices, then any hint or note.
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

    // runs when a letter is pressed. a right answer gives points and moves on, a wrong one costs a heart.
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

    // goes to the next question, or ends the round with a win if that was the last one.
    private void advance() {
        if (current + 1 >= questions.length) {
            endRound(true);
        } else {
            current++;
            showQuestion();
        }
    }

    // ends the round and shows the win or game over dialog.
    private void endRound(boolean won) {
        finished = true;
        locked = true;
        if (won) {
            saveStore.clear(windowTitle);   // a cleared module has nothing left to resume. a loss keeps the saved checkpoint
        }
        refreshControls();
        if (won) {
            String unlocked = onWin == null ? null : onWin.get();
            textArea.setText("VICTORY!\n\nYou cleared every question in this topic.\n\nFinal score: " + score
                    + (unlocked == null ? "" : "\n\n" + unlocked));
            textArea.setCaretPosition(0);
            if (onNextLevel != null) {
                final int heartsLeft = lives;   // the next module starts with exactly these hearts
                final int scoreSoFar = score;   // and keeps adding to this score
                overlay.show(new String[]{"You Win!"}, unlocked, score,
                        new String[]{"Next Level", "Back to Main Menu"},
                        new Runnable[]{() -> onNextLevel.accept(heartsLeft, scoreSoFar), this::backToMainMenu});
            } else {
                // nothing comes after this topic (the last module), so offer the main menu instead of exit
                overlay.show(new String[]{"You Win!"}, unlocked, score,
                        new String[]{"Start Again", "Back to Main Menu"},
                        new Runnable[]{this::startRound, this::backToMainMenu});
            }
        } else {
            if (onLose != null) {
                onLose.run();   // losing sends the whole run back to module 1
            }
            textArea.setText("GAME OVER\n\nYou ran out of hearts.\n\nFinal score: " + score);
            textArea.setCaretPosition(0);
            overlay.show(new String[]{"GAME OVER!"}, null, score,
                    new String[]{"Start Again", "Back to Main Menu"},
                    new Runnable[]{this::restartAfterLoss, this::backToMainMenu});
        }
        repaint();
    }

    // after a game over: goes back to the first module with full hearts, or just restarts this topic if there is no first module.
    private void restartAfterLoss() {
        if (onRestart != null) {
            onRestart.run();
        } else {
            startRound();
            offerResume();   // the topic may still have a saved game from before the loss
        }
    }

    // peek and copy can be used once each, and only while a question is open.
    private boolean lifelineAvailable(boolean thisOneUsed) {
        return !(finished || locked || thisOneUsed || overlay.isVisible());
    }

    // save is never used up. it works any time the round is still going and the player has hearts.
    private boolean saveAvailable() {
        return !finished && !locked && lives > 0 && !overlay.isVisible();
    }

    // peek: shows the hint for the current question.
    private void usePeek() {
        if (!lifelineAvailable(peekUsed)) {
            return;
        }
        peekUsed = true;
        note = "Hint: " + questions[current].getHint();
        refreshText();
        refreshControls();
    }

    // copy: crosses out two wrong choices.
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

    // save: asks first. "yes" saves the game, "no" goes back to the question.
    private void useSave() {
        if (!saveAvailable()) {
            return;
        }
        overlay.show(new String[]{"Do you want to save", "your Score?"}, null, score,
                new String[]{"Yes", "No"},
                new Runnable[]{this::confirmSave, () -> { overlay.close(); refreshControls(); }});
    }

    // writes the game to the save file, then shows the "saved" dialog.
    private void confirmSave() {
        SavedGame game = new SavedGame();
        game.score = score;
        game.current = current;
        game.lives = lives;
        game.peek = peekUsed;
        game.copy = copyUsed;
        game.hintShown = !note.isEmpty();
        game.removed = removed.clone();
        game.questions = questions;
        saveStore.write(windowTitle, game);
        overlay.show(new String[]{"Current Score", "Saved!"}, null, -1,
                new String[]{"Back to Main Menu", "Keep Playing"},
                new Runnable[]{this::backToMainMenu, () -> { overlay.close(); refreshControls(); }});
    }

    // goes back to the main menu.
    private void backToMainMenu() {
        stopTimer();
        overlay.close();
        if (frame != null) {
            frame.showScreen(MainFrame.SPLASH);
        }
    }

    // updates the hearts, which buttons are enabled and which marks are showing.
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