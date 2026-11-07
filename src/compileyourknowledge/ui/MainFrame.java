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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

    /** the main window. 
     * it holds every screen as a card and switches between them. */

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
    private final boolean[] theoreticalUnlocked =
            new boolean[Math.max(8, questionBank.topics("Theoretical").size())];
    private final SaveStore progressStore = SaveStore.inUserHome();
    private final SettingsOverlay settings = new SettingsOverlay();
    private final TopicPanel theoreticalPanel;
    private final TopicPanel practicalPanel;
    private final QuestionPanel questionPanel;
    private boolean fullScreen = false;

    public MainFrame() {
        super("Compile Your Knowledge");
        preloadArt();

        // module 1 is always open. the others stay unlocked from earlier sessions
        int unlocked = Math.max(1, progressStore.readUnlockedModules());
        for (int i = 0; i < unlocked && i < theoreticalUnlocked.length; i++) {
            theoreticalUnlocked[i] = true;
        }
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cards.add(new SplashPanel(this), SPLASH);
        cards.add(new PlayPanel(this), START);
        
        theoreticalPanel = new TopicPanel(this, "Theoretical", true, questionBank);
        practicalPanel = new TopicPanel(this, "Practical", false, questionBank);
        
        cards.add(theoreticalPanel, THEORETICAL);
        cards.add(practicalPanel, PRACTICAL);

        // one question window for the whole game. startTopic() refills it for each topic
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

        // the sound settings float above every screen as the glass pane.
        // setGlassPane hides it, so it has to be shown again

        setGlassPane(settings);
        settings.setVisible(true);
        applyCustomCursor();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        setupKeys();

        showScreen(SPLASH);
        setFullScreen(true);
        SoundManager.installClickSound();
        SoundManager.playOpening();
    }

    // sets up the keyboard shortcuts: f11 for full screen, esc to close things, m for sound
    private void setupKeys() {
        bindKey(KeyEvent.VK_F11, "toggleFullScreen", () -> setFullScreen(!fullScreen));
        bindKey(KeyEvent.VK_ESCAPE, "leaveFullScreen", () -> {
            if (settings.isOpen()) {
                settings.close();   // esc closes the sound dialog first
            } else if (fullScreen) {
                setFullScreen(false);
            }
        });
        bindKey(KeyEvent.VK_M, "toggleSound", settings::toggle);
    }

    // makes a key press run an action anywhere in the window
    private void bindKey(int keyCode, String name, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

        /**
         * loads and crops all the pictures at the same time before the screens are built
         * the button pictures are on huge transparent canvases, so doing them one by one made start-up slow
         */

    private static void preloadArt() {
        List<String> art = new ArrayList<>(List.of(
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
            if (image != null && path.contains("/buttons/")) {
                // keep only the cropped button. the huge original isn't needed again
                ImageLibrary.replace(path, ImageButton.prepare(image));
            }
        });
    }

    // true if the theoretical module at this position can be played. 
    public boolean isTheoreticalUnlocked(int index) {
        return index >= 0 && index < theoreticalUnlocked.length && theoreticalUnlocked[index];
    }

    /**
     * starts one stage with up to 10 questions from the topic: easy first, then medium, then hard.
     * questions are random inside each difficulty and the four choices are shuffled.
     */

    public void startTopic(String branch, String topic) {
        startTopic(branch, topic, QuestionPanel.MAX_LIVES, 0, false);
    }

    // same as above, but the player starts with this many hearts and this score (carried over from the last module)
    private void startTopic(String branch, String topic, int startLives, int startScore, boolean resumeNow) {
        boolean theoretical = branch.equalsIgnoreCase("Theoretical");
        List<String> topics = questionBank.topics(branch);
        
        int topicIndex = topics.indexOf(topic);
       
        String title = titleFor(branch, topic);

        // winning a theoretical module unlocks the next one
        Supplier<String> onWin = theoretical ? () -> unlockAfter(topicIndex) : null;

        // "next level" after a win opens the following module (theoretical only)
       
        int next = topicIndex + 1;
        BiConsumer<Integer, Integer> onNext = theoretical && topicIndex >= 0 && next < topics.size()
                && next < theoreticalUnlocked.length
                ? (hearts, carriedScore) -> startTopic(branch, topics.get(next), hearts, carriedScore, false)
                : null;

        // losing sends the player back to module 1 with full hearts (theoretical only)
        Runnable onRestart = theoretical && topicIndex > 0
                ? () -> startTopic(branch, topics.get(0), QuestionPanel.MAX_LIVES, 0, false)
                : null;

        // losing also locks the modules again, so the run starts over from module 1
        Runnable onLose = theoretical ? this::resetTheoreticalProgress : null;

        questionPanel.startTopic(title,
                () -> questionBank.drawStage(branch, topic, QUESTIONS_PER_STAGE),
                theoretical ? THEORETICAL : PRACTICAL, onWin, onNext, onRestart, onLose, startLives, startScore, resumeNow);
        showScreen(QUESTION);
    }

    // the name a topic is saved under, like "Module 3 - Loops". the save file and the question window both use it
    private String titleFor(String branch, String topic) {
        boolean theoretical = branch.equalsIgnoreCase("Theoretical");
        int topicIndex = questionBank.topics(branch).indexOf(topic);
        return theoretical && topicIndex >= 0
                ? "Module " + (topicIndex + 1) + " - " + topic
                : branch + " - " + topic;
    }

    // opens the theoretical screen and asks about saved progress before the player picks a module
    public void openTheoretical() {
        showScreen(THEORETICAL);
        theoreticalPanel.offerContinue();
    }

    // finds the topic in this branch that has a saved game. if there are several, the furthest one. null if none
    String findSavedTopic(String branch) {
        List<String> topics = questionBank.topics(branch);
        for (int i = topics.size() - 1; i >= 0; i--) {
            if (progressStore.read(titleFor(branch, topics.get(i))) != null) {
                return topics.get(i);
            }
        }
        return null;
    }

    // the score that was saved for this topic
    int savedScore(String branch, String topic) {
        SavedGame saved = progressStore.read(titleFor(branch, topic));
        return saved == null ? 0 : saved.score;
    }

    // opens this topic and loads its saved game straight away, without asking again.
    // the modules up to this one are unlocked again, because the save was made while they were open
    void continueSaved(String branch, String topic) {
        int index = questionBank.topics(branch).indexOf(topic);
        if (branch.equalsIgnoreCase("Theoretical") && index >= 0) {
            for (int i = 0; i <= index && i < theoreticalUnlocked.length; i++) {
                theoreticalUnlocked[i] = true;
            }
            progressStore.writeUnlockedModules(unlockedCount());
            theoreticalPanel.refreshLocks();
        }
        startTopic(branch, topic, QuestionPanel.MAX_LIVES, 0, true);   // the saved game brings back its own hearts and score
    }

    // "no" to continuing: deletes every saved game in this branch and sends the theoretical progress back to module 1
    void discardProgress(String branch) {
        for (String topic : questionBank.topics(branch)) {
            progressStore.clear(titleFor(branch, topic));
        }
        if (branch.equalsIgnoreCase("Theoretical")) {
            resetTheoreticalProgress();
        }
    }

    // locks every theoretical module except module 1 and remembers that. runs when the player loses
    void resetTheoreticalProgress() {
        Arrays.fill(theoreticalUnlocked, false);
        theoreticalUnlocked[0] = true;
        progressStore.writeUnlockedModules(1);
        theoreticalPanel.refreshLocks();
    }

    // how many theoretical modules are open right now
    int unlockedCount() {
        int count = 0;
        for (boolean open : theoreticalUnlocked) {
            if (open) {
                count++;
            }
        }
        return count;
    }

    // true if the player has opened more than module 1, so there is progress to continue
    boolean hasTheoreticalProgress() {
        return unlockedCount() > 1;
    }

    // clearing module i unlocks module i + 1. returns a line for the victory text, or null if nothing new unlocked. 
    private String unlockAfter(int topicIndex) {
        int next = topicIndex + 1;
        if (topicIndex >= 0 && next < theoreticalUnlocked.length && !theoreticalUnlocked[next]) {
            theoreticalUnlocked[next] = true;
            progressStore.writeUnlockedModules(next + 1);
            
            theoreticalPanel.refreshLocks();
            return "Module " + (next + 1) + " unlocked!";
        }
        return null;
    }

    // switches to the screen with this name. 
    public void showScreen(String name) {
        if (THEORETICAL.equals(name)) {
            theoreticalPanel.refreshLocks();
        }
        cardLayout.show(cards, name);
    }

    // turns full screen on or off 
    public void setFullScreen(boolean on) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        fullScreen = on;
        if (on) {
            dispose();
            setUndecorated(true);
            if (device.isFullScreenSupported()) {
                device.setFullScreenWindow(this);
            } else {
                setExtendedState(MAXIMIZED_BOTH);
                setVisible(true);
            }
        } else {
            if (device.getFullScreenWindow() == this) {
                device.setFullScreenWindow(null);
            }
            dispose();
            setUndecorated(false);
            setExtendedState(NORMAL);
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }
        requestFocus();
        applyCustomCursor();
    }

    // customized cursor 
    private void applyCustomCursor() {
        BufferedImage img = ImageLibrary.load("resources/images/cursor.png");
        if (img == null) {
            return;
        }
        Cursor custom = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(0, 0), "custom");
        setCursor(custom);
        cards.setCursor(custom);
    }
}
