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
    private boolean fullScreen = false;

    public MainFrame() {
        super("Compile Your Knowledge");
        theoreticalUnlocked[0] = true;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cards.add(new SplashPanel(this), SPLASH);
        cards.add(new PlayPanel(this), START);
        theoreticalPanel = new TopicPanel(this, "Theoretical", true, questionBank);
        practicalPanel = new TopicPanel(this, "Practical", false, questionBank);
        cards.add(theoreticalPanel, THEORETICAL);
        cards.add(practicalPanel, PRACTICAL);
        cards.add(new QuestionPanel(this, "Question", () -> new Question[0]), QUESTION);
        cards.add(new ScreenPanel(this, "resources/images/HowToPlayScreen.png"), HOW_TO_PLAY);

        ScreenPanel developers = new ScreenPanel(this, "resources/images/CreditsDevelopers.png");
        developers.addHotspot(1726, 662, 151, 151, "Next", () -> showScreen(CREDITS_INSPIRATION));
        ScreenPanel inspiration = new ScreenPanel(this, "resources/images/CreditsInspiration.png");
        inspiration.addHotspot(123, 662, 151, 151, "Previous", () -> showScreen(CREDITS));
        cards.add(developers, CREDITS);
        cards.add(inspiration, CREDITS_INSPIRATION);

        cards.setPreferredSize(new Dimension(1000, 707));
        setContentPane(cards);
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
            @Override public void actionPerformed(ActionEvent e) { if (fullScreen) setFullScreen(false); }
        });

        showScreen(SPLASH);
        setFullScreen(true);
        SoundManager.playOpening();
    }

    public boolean isTheoreticalUnlocked(int index) {
        return index >= 0 && index < theoreticalUnlocked.length && theoreticalUnlocked[index];
    }

    /** Starts one lesson/language stage. Each round asks up to 10 randomly selected questions. */
    public void startTopic(String branch, String topic) {
        int topicIndex = questionBank.topics(branch).indexOf(topic);
        questionPanel = new QuestionPanel(this, branch + " - " + topic,
                () -> questionBank.drawTopic(branch, topic, QUESTIONS_PER_STAGE),
                won -> {
                    if (won && branch.equalsIgnoreCase("Theoretical") && topicIndex >= 0
                            && topicIndex + 1 < theoreticalUnlocked.length) {
                        theoreticalUnlocked[topicIndex + 1] = true;
                    }
                    theoreticalPanel.refreshLocks();
                });
        cards.add(questionPanel, QUESTION);
        showScreen(QUESTION);
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
