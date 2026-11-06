package compileyourknowledge.ui;

import compileyourknowledge.model.Question;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Saved checkpoints live in one properties file, one entry set per topic, so they survive closing the
 * game. Each topic's keys start with "&lt;topic title&gt;|".
 */
final class SaveStore {

    private final File file;

    SaveStore(File file) {
        this.file = file;
    }

    /** The default location: a hidden folder in the user's home directory. */
    static SaveStore inUserHome() {
        return new SaveStore(new File(System.getProperty("user.home"), ".compileyourknowledge/save.properties"));
    }

    /** Saves the round as it stands at the start of the current question. */
    void write(String topic, SavedGame game) {
        Properties props = load();
        props.entrySet().removeIf(e -> String.valueOf(e.getKey()).startsWith(key(topic, "")));
        props.setProperty(key(topic, "score"), String.valueOf(game.score));
        props.setProperty(key(topic, "current"), String.valueOf(game.current));
        props.setProperty(key(topic, "lives"), String.valueOf(game.lives));
        props.setProperty(key(topic, "peek"), String.valueOf(game.peek));
        props.setProperty(key(topic, "copy"), String.valueOf(game.copy));
        props.setProperty(key(topic, "count"), String.valueOf(game.questions.length));
        for (int i = 0; i < game.questions.length; i++) {
            Question q = game.questions[i];
            String k = "q" + i + ".";
            props.setProperty(key(topic, k + "topic"), q.getTopic() == null ? "" : q.getTopic());
            props.setProperty(key(topic, k + "difficulty"), String.valueOf(q.getDifficulty()));
            props.setProperty(key(topic, k + "prompt"), q.getPrompt());
            for (int c = 0; c < 4; c++) {
                props.setProperty(key(topic, k + "choice" + c), q.getChoice(c));
            }
            props.setProperty(key(topic, k + "correct"), String.valueOf(q.getCorrectIndex()));
            props.setProperty(key(topic, k + "hint"), q.getHint() == null ? "" : q.getHint());
        }
        store(props);
    }

    /** The saved game for this topic, or null if there is none (or it cannot be read). */
    SavedGame read(String topic) {
        Properties props = load();
        if (props.getProperty(key(topic, "count")) == null) {
            return null;
        }
        try {
            SavedGame saved = new SavedGame();
            saved.score = Integer.parseInt(props.getProperty(key(topic, "score")));
            saved.current = Integer.parseInt(props.getProperty(key(topic, "current")));
            saved.lives = Integer.parseInt(props.getProperty(key(topic, "lives")));
            saved.peek = Boolean.parseBoolean(props.getProperty(key(topic, "peek")));
            saved.copy = Boolean.parseBoolean(props.getProperty(key(topic, "copy")));
            int count = Integer.parseInt(props.getProperty(key(topic, "count")));
            saved.questions = new Question[count];
            for (int i = 0; i < count; i++) {
                String k = "q" + i + ".";
                String[] choices = new String[4];
                for (int c = 0; c < 4; c++) {
                    choices[c] = props.getProperty(key(topic, k + "choice" + c));
                }
                saved.questions[i] = new Question(
                        props.getProperty(key(topic, k + "topic")),
                        Integer.parseInt(props.getProperty(key(topic, k + "difficulty"))),
                        props.getProperty(key(topic, k + "prompt")),
                        choices,
                        Integer.parseInt(props.getProperty(key(topic, k + "correct"))),
                        props.getProperty(key(topic, k + "hint")));
            }
            if (count == 0 || saved.current < 0 || saved.current >= count || saved.lives <= 0) {
                return null;
            }
            return saved;
        } catch (RuntimeException e) {   // missing key, bad number, null choice: treat as no save
            return null;
        }
    }

    void clear(String topic) {
        Properties props = load();
        if (props.entrySet().removeIf(e -> String.valueOf(e.getKey()).startsWith(key(topic, "")))) {
            store(props);
        }
    }

    private static String key(String topic, String name) {
        return topic + "|" + name;
    }

    private Properties load() {
        Properties props = new Properties();
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Could not read save file: " + e);
            }
        }
        return props;
    }

    private void store(Properties props) {
        try {
            File dir = file.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "Compile Your Knowledge - saved games");
            }
        } catch (IOException e) {
            System.err.println("Could not write save file: " + e);
        }
    }
}
