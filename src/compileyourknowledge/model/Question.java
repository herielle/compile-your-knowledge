package compileyourknowledge.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

    /** one multiple choice question. it always has four choices (a to d). */

public class Question {

    private final String topic;
    private final int difficulty;
    private final String prompt;
    private final String[] choices;
    private final int correctIndex;
    private final String hint;

    public Question(String topic, int difficulty, String prompt, String[] choices, int correctIndex, String hint) {
        if (choices == null || choices.length != 4) {
            throw new IllegalArgumentException("A question needs exactly four choices.");
        }
        for (String choice : choices) {
            if (choice == null) {
                throw new IllegalArgumentException("A choice must not be missing.");
            }
        }
        if (prompt == null) {
            throw new IllegalArgumentException("A question needs a prompt.");
        }
        if (correctIndex < 0 || correctIndex > 3) {
            throw new IllegalArgumentException("The correct answer must be 0 to 3.");
        }
        this.topic = topic;
        this.difficulty = difficulty;
        this.prompt = prompt;
        this.choices = choices.clone();
        this.correctIndex = correctIndex;
        this.hint = hint;
    }

    public String getTopic() {
        return topic;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getChoice(int i) {
        return choices[i];
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getHint() {
        return hint;
    }

    // makes a copy with the four choices in a random order. the correct index moves with its answer
    public Question shuffled(Random random) {
        Integer[] order = {0, 1, 2, 3};
        Collections.shuffle(Arrays.asList(order), random);
        String[] mixed = new String[4];
        int newCorrect = 0;
        for (int i = 0; i < 4; i++) {
            mixed[i] = choices[order[i]];
            if (order[i] == correctIndex) {
                newCorrect = i;
            }
        }
        return new Question(topic, difficulty, prompt, mixed, newCorrect, hint);
    }
}
