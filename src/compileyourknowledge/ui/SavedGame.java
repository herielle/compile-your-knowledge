package compileyourknowledge.ui;

import compileyourknowledge.model.Question;

    /** everything needed to continue a round later, including the exact questions that were picked */

final class SavedGame {
    int score;
    int current;
    int lives;
    boolean peek;
    boolean copy;
    boolean hintShown; // the peek hint was showing on the current question
    boolean[] removed = new boolean[4]; // choices already crossed out on the current question
    Question[] questions;
}
