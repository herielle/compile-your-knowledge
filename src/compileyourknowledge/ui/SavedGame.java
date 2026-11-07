package compileyourknowledge.ui;

import compileyourknowledge.model.Question;

/** Everything needed to carry on a round later, including the exact questions that were drawn. */
final class SavedGame {
    int score;
    int current;
    int lives;
    boolean peek;
    boolean copy;
    boolean hintShown;                 // the Peek hint was showing on the current question
    boolean[] removed = new boolean[4]; // choices already crossed out on the current question
    Question[] questions;
}
