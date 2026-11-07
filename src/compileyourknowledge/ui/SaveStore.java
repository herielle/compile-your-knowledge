package compileyourknowledge.ui;

import compileyourknowledge.model.Question;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

    /**
     * keeps saved games in one properties file so they are still there after closing the game
     * each topic has its own set of keys that start with "topic title|".
     */

final class SaveStore {

    private static final String PROGRESS_KEY = "progress.unlocked";

    private final File file;

    SaveStore(File file) {
        this.file = file;
    }

    // the normal save location: a hidden folder in the user's home directory. 
    static SaveStore inUserHome() {
        return new SaveStore(new File(System.getProperty("user.home"), ".compileyourknowledge/save.properties"));
    }

    // saves the round as it is at the start of the current question
    void write(String topic, SavedGame game) {
        Properties props = load();
        props.entrySet().removeIf(e -> String.valueOf(e.getKey()).startsWith(key(topic, "")));
        props.setProperty(key(topic, "score"), String.valueOf(game.score));
        props.setProperty(key(topic, "current"), String.valueOf(game.current));
        props.setProperty(key(topic, "lives"), String.valueOf(game.lives));
        props.setProperty(key(topic, "peek"), String.valueOf(game.peek));
        props.setProperty(key(topic, "copy"), String.valueOf(game.copy));
        props.setProperty(key(topic, "hintShown"), String.valueOf(game.hintShown));

        // crossed out choices are stored as 1s and 0s, like 0110
        StringBuilder removed = new StringBuilder();
        for (boolean r : game.removed) {
            removed.append(r ? '1' : '0');
        }
        props.setProperty(key(topic, "removed"), removed.toString());

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

    // gets the saved game for this topic. returns null if there is none or it can't be read
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
            saved.hintShown = Boolean.parseBoolean(props.getProperty(key(topic, "hintShown")));
            String removed = props.getProperty(key(topic, "removed"), "0000");   // older saves don't have this
            for (int i = 0; i < 4 && i < removed.length(); i++) {
                saved.removed[i] = removed.charAt(i) == '1';
            }
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
        } catch (RuntimeException e) { // missing key or bad number means the save is broken, so treat it as no save
            return null;
        }
    }

    // how many theoretical modules are unlocked so far (always at least 1)
    int readUnlockedModules() {
        try {
            return Math.max(1, Integer.parseInt(load().getProperty(PROGRESS_KEY, "1").trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // saves how many theoretical modules are unlocked
    void writeUnlockedModules(int count) {
        Properties props = load();
        props.setProperty(PROGRESS_KEY, String.valueOf(Math.max(1, count)));
        store(props);
    }

    // deletes the saved game for this topic 
    void clear(String topic) {
        Properties props = load();
        if (props.entrySet().removeIf(e -> String.valueOf(e.getKey()).startsWith(key(topic, "")))) {
            store(props);
        }
    }

    // builds the full key for a topic, like "Module 1 - Basics|score". 
    private static String key(String topic, String name) {
        return topic + "|" + name;
    }

    // reads the whole save file. returns an empty set if the file doesn't exist yet
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

    // writes the whole save file. 
    private void store(Properties props) {
        try {
            File dir = file.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            
            // write to a temp file first, then swap it in, so a crash can't leave a half written save
            File temp = new File(file.getPath() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                props.store(out, "Compile Your Knowledge - saved games");
            }
            try {
                Files.move(temp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Could not write save file: " + e);
        }
    }
}
