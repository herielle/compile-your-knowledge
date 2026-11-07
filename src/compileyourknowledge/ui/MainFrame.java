package compileyourknowledge.ui;

import compileyourknowledge.model.Question;
import compileyourknowledge.model.QuestionBank;
import compileyourknowledge.util.ImageLibrary;
import compileyourknowledge.util.SoundManager;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class MainFrame extends JFrame {
    public static final String SPLASH = "SPLASH";
    public static final String START = "START";
    public static final String THEORETICAL = "THEORETICAL";
    public static final String PRACTICAL = "PRACTICAL";
    public static final String HOW_TO_PLAY = "HOW_TO_PLAY";
    public static final String CREDITS = "CREDITS";
    public static final String CREDITS_INSPIRATION = "CREDITS_INSPIRATION";
    public static final String QUESTION = "QUESTION";

    private static final int QUESTIONS_PER_STAGE = 10;

    private final QuestionBank questionBank = QuestionBank.loadDefault();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final boolean[] theoreticalUnlocked = new boolean[8];
    private TopicPanel theoreticalPanel;
    private TopicPanel practicalPanel;
    private QuestionPanel questionPanel;
    private final SettingsOverlay settings = new SettingsOverlay();
    private boolean fullScreen = false;

    public MainFrame() {
        super("Compile Your Knowledge");
        preloadArt();
        theoreticalUnlocked[0] = true;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cards.add(new SplashPanel(this), SPLASH);
        cards.add(new PlayPanel(this), START);
        theoreticalPanel = new TopicPanel(this, "Theoretical", true, questionBank);
        practicalPanel = new TopicPanel(this, "Practical", false, questionBank);
        cards.add(theoreticalPanel, THEORETICAL);
        cards.add(practicalPanel, PRACTICAL);
        // One question window for the whole game; startTopic() refills it for each topic.
        questionPanel = new QuestionPanel(this, "Question", () -> new Question[0]);
        cards.add(questionPanel, QUESTION);
        cards.add(new ScreenPanel(this, "resources/images/screens/HowToPlayScreen.png"), HOW_TO_PLAY);

        ScreenPanel developers = new ScreenPanel(this, "resources/images/screens/CreditsDevelopers.png");
        developers.addHotspot(1726, 662, 151, 151, "Next", () -> showScreen(CREDITS_INSPIRATION));
        ScreenPanel inspiration = new ScreenPanel(this, "resources/images/screens/CreditsInspiration.png");
        inspiration.addHotspot(123, 662, 151, 151, "Previous", () -> showScreen(CREDITS));
        cards.add(developers, CREDITS);
        cards.add(inspiration, CREDITS_INSPIRATION);

        cards.setPreferredSize(new Dimension(1000, 707));
        setContentPane(cards);
        // The sound settings float above every screen as the glass pane (setGlassPane hides it, so show it again).
        setGlassPane(settings);
        settings.setVisible(true);
        applyCustomCursor();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullScreen");
        getRootPane().getActionMap().put("toggleFullScreen", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setFullScreen(!fullScreen); }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "leaveFullScreen");
        getRootPane().getActionMap().put("leaveFullScreen", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (settings.isOpen()) settings.close();   // Esc closes the sound dialog first
                else if (fullScreen) setFullScreen(false);
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "toggleSound");
        getRootPane().getActionMap().put("toggleSound", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { settings.toggle(); }
        });

        showScreen(SPLASH);
        setFullScreen(true);
        SoundManager.installClickSound();
        SoundManager.playOpening();
    }

    /**
     * Decodes and crops all artwork in parallel before the screens are built. The button pictures sit on
     * very large transparent canvases, so doing this one after another made start-up slow.
     */
    private static void preloadArt() {
        java.util.List<String> art = new java.util.ArrayList<>(java.util.List.of(
                "resources/images/screens/background.png", "resources/images/screens/MainScreen.png",
                "resources/images/screens/PlayScreen.png", "resources/images/screens/HowToPlayScreen.png",
                "resources/images/screens/CreditsDevelopers.png", "resources/images/screens/CreditsInspiration.png",
                "resources/images/heart.png", "resources/images/correct.png", "resources/images/wrong.png",
                "resources/images/cursor.png"));
        String[] buttons = {"start", "howToPlay", "credits", "exit", "theoretical", "practical",
                "a_letter", "b_letter", "c_letter", "d_letter", "peek_button", "copy_button", "save_button"};
        for (String name : buttons) {
            art.add("resources/images/buttons/" + name + ".png");
        }
        for (int i = 1; i <= 8; i++) {
            art.add("resources/images/buttons/theoreticalButtons/module" + i + ".png");
            if (i > 1) {
                art.add("resources/images/buttons/theoreticalButtons/module" + i + "-locked.png");
            }
        }
        for (String name : new String[]{"C", "java", "pyton", "haskell"}) {
            art.add("resources/images/buttons/practicalButtons/" + name + ".png");
        }
        art.parallelStream().forEach(path -> {
            BufferedImage image = ImageLibrary.load(path);
            if (path.contains("/buttons/")) {
                ImageButton.prepare(image);
            }
        });
    }

    public boolean isTheoreticalUnlocked(int index) {
        return index >= 0 && index < theoreticalUnlocked.length && theoreticalUnlocked[index];
    }

    /**
     * Starts one stage: up to 10 questions from the topic, ordered easy -> medium -> hard, random inside
     * each difficulty, with the four choices shuffled. Reuses the single question window.
     */
    public void startTopic(String branch, String topic) {
        startTopic(branch, topic, QuestionPanel.MAX_LIVES);
    }

    /** Same, but the player begins with the given number of hearts (carried over from the last module). */
    private void startTopic(String branch, String topic, int startLives) {
        boolean theoretical = branch.equalsIgnoreCase("Theoretical");
        int topicIndex = questionBank.topics(branch).indexOf(topic);
        String title = theoretical && topicIndex >= 0
                ? "Module " + (topicIndex + 1) + " - " + topic
                : branch + " - " + topic;
        Supplier<String> onWin = theoretical ? () -> unlockAfter(topicIndex) : null;
        // After a win, "Next Level" opens the following module (only for the theoretical modules).
        java.util.List<String> topics = questionBank.topics(branch);
        int next = topicIndex + 1;
        IntConsumer onNext = theoretical && topicIndex >= 0 && next < topics.size()
                && next < theoreticalUnlocked.length
                ? hearts -> startTopic(branch, topics.get(next), hearts)
                : null;
        questionPanel.startTopic(title,
                () -> questionBank.drawStage(branch, topic, QUESTIONS_PER_STAGE),
                theoretical ? THEORETICAL : PRACTICAL, onWin, onNext, startLives);
        showScreen(QUESTION);
    }

    /** Clearing module i opens module i + 1. Returns a line for the Victory text, or null. */
    private String unlockAfter(int topicIndex) {
        int next = topicIndex + 1;
        if (topicIndex >= 0 && next < theoreticalUnlocked.length && !theoreticalUnlocked[next]) {
            theoreticalUnlocked[next] = true;
            theoreticalPanel.refreshLocks();
            return "Module " + (next + 1) + " unlocked!";
        }
        return null;
    }

    public void showScreen(String name) {
        if (THEORETICAL.equals(name)) theoreticalPanel.refreshLocks();
        cardLayout.show(cards, name);
    }

    public void setFullScreen(boolean on) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        fullScreen = on;
        if (on) {
            dispose(); setUndecorated(true);
            if (device.isFullScreenSupported()) device.setFullScreenWindow(this);
            else { setExtendedState(MAXIMIZED_BOTH); setVisible(true); }
        } else {
            if (device.getFullScreenWindow() == this) device.setFullScreenWindow(null);
            dispose(); setUndecorated(false); setExtendedState(NORMAL); pack(); setLocationRelativeTo(null); setVisible(true);
        }
        requestFocus(); applyCustomCursor();
    }

    private void applyCustomCursor() {
        BufferedImage img = ImageLibrary.load("resources/images/cursor.png");
        if (img == null) return;
        Cursor custom = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(0, 0), "custom");
        setCursor(custom); cards.setCursor(custom);
    }
}
