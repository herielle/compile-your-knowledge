package compileyourknowledge.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

public class MainFrame extends JFrame {

    public static final String SPLASH = "SPLASH";
    public static final String START = "START";
    public static final String HOW_TO_PLAY = "HOW_TO_PLAY";
    public static final String CREDITS = "CREDITS";

    private static final String HOW_TO_PLAY_TEXT = """
            1. Click Start, then choose Theoretical or Practical.
            2. Click an unlocked topic to load its questions. Locked topics open after the previous topic is cleared.
            3. Read the question and click one of the four answers (A to D).
            4. Use one lifeline per game before answering: Peek shows a hint, Copy removes two wrong answers, and Save banks your score as a checkpoint.
            5. A correct answer moves you to a harder question. A wrong answer disables that choice and costs one heart.
            6. Clear every question to win. Lose all three hearts and the round ends in Game Over.
            """;

    private static final String CREDITS_TEXT = """
            Developers
            Herielle Margallo
            Lorenz Ed Ocampo

            Inspiration and References
            The game show "Are You Smarter Than a 5th Grader?" and the Windows 95 and Frutiger Aero looks.

            Assets, Libraries, and Tools
            Image and visual references from Pinterest creators and authors.
            Visual assets and UI elements from Canva.

            CMSC 13: Survey of Programming Paradigms, University of the Philippines Tacloban College.
            """;

    private static final String START_TEXT = """
            Placeholder screen.

            The Theoretical and Practical program-selection window will appear here in a later milestone.
            """;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public MainFrame() {
        super("Compile Your Knowledge");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cards.add(new SplashPanel(this), SPLASH);
        cards.add(new InfoPanel(this, "Compile Your Knowledge", START_TEXT), START);
        cards.add(new InfoPanel(this, "How to Play", HOW_TO_PLAY_TEXT), HOW_TO_PLAY);
        cards.add(new InfoPanel(this, "Credits", CREDITS_TEXT), CREDITS);

        cards.setPreferredSize(new Dimension(1000, 707)); // same proportions as the 2000x1414 artwork
        setContentPane(cards);
        
        pack();
        
        setResizable(false);
        setLocationRelativeTo(null); // center on screen

        showScreen(SPLASH);
    }

    /** Switches to the card registered under the given name. */
    public void showScreen(String name) {
        cardLayout.show(cards, name);
    }
}