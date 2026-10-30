package compileyourknowledge.model;

/** One multiple-choice question. Answers are always four choices (A to D). */
public class Question {

    private final String topic;
    private final int difficulty;
    private final String prompt;
    private final String[] choices;
    private final int correctIndex;
    private final String hint;

    public Question(String topic, int difficulty, String prompt, String[] choices, int correctIndex, String hint) {
        if (choices.length != 4) {
            throw new IllegalArgumentException("A question needs exactly four choices.");
        }
        this.topic = topic;
        this.difficulty = difficulty;
        this.prompt = prompt;
        this.choices = choices.clone();
        this.correctIndex = correctIndex;
        this.hint = hint;
    }

    public String getTopic() { return topic; }
    public int getDifficulty() { return difficulty; }
    public String getPrompt() { return prompt; }
    public String getChoice(int i) { return choices[i]; }
    public int getCorrectIndex() { return correctIndex; }
    public String getHint() { return hint; }
}
