package compileyourknowledge.model;

import compileyourknowledge.util.CsvReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Loads every question from a CSV file and hands out rounds.
 *
 * Expected columns (any order, header row required):
 * branch, topic, difficulty, prompt, choiceA, choiceB, choiceC, choiceD, answer, hint
 *
 * A round is always ordered from the lowest difficulty to the highest. Inside each difficulty the
 * order is freshly shuffled every time a round is drawn, so no two rounds play out the same way.
 */
public final class QuestionBank {

    public static final String DEFAULT_PATH = "resources/questions/questions.csv";

    private static final String[] REQUIRED = {
        "branch", "topic", "difficulty", "prompt", "choicea", "choiceb", "choicec", "choiced", "answer"
    };

    // branch (lower case) -> topic (lower case) -> questions, both in CSV order.
    private final Map<String, Map<String, List<Question>>> byBranch = new LinkedHashMap<>();
    private final Random random;

    public QuestionBank() {
        this(new Random());
    }

    /** Pass a seeded Random for repeatable shuffles (handy for tests). */
    public QuestionBank(Random random) {
        this.random = random;
    }

    // ------------------------------------------------------------------ loading

    /** Loads the bundled CSV. Returns an empty bank, with a warning, when it cannot be read. */
    public static QuestionBank loadDefault() {
        QuestionBank bank = new QuestionBank();
        try (Reader reader = open(DEFAULT_PATH)) {
            bank.load(reader);
        } catch (IOException e) {
            System.err.println("WARNING: could not read '" + DEFAULT_PATH + "': " + e.getMessage());
        }
        return bank;
    }

    private static Reader open(String path) throws IOException {
        InputStream in = QuestionBank.class.getResourceAsStream("/" + path);
        if (in != null) {
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        File file = new File("src/" + path);
        if (file.exists()) {
            return Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
        }
        throw new IOException("file not found on the classpath or under src/");
    }

    /** Reads questions from any reader. Bad rows are reported and skipped, never fatal. */
    public void load(Reader source) throws IOException {
        Reader in = source.markSupported() ? source : new BufferedReader(source);
        List<String[]> records = CsvReader.parse(in);
        if (records.isEmpty()) {
            System.err.println("WARNING: the question file is empty.");
            return;
        }

        Map<String, Integer> col = new LinkedHashMap<>();
        String[] header = records.get(0);
        for (int i = 0; i < header.length; i++) {
            col.put(header[i].trim().toLowerCase(Locale.ROOT), i);
        }
        for (String name : REQUIRED) {
            if (!col.containsKey(name)) {
                System.err.println("WARNING: the question file has no '" + name + "' column; nothing was loaded.");
                return;
            }
        }

        int loaded = 0;
        for (int r = 1; r < records.size(); r++) {
            String[] row = records.get(r);
            try {
                Question q = toQuestion(row, col);
                String branch = cell(row, col, "branch").toLowerCase(Locale.ROOT);
                byBranch.computeIfAbsent(branch, k -> new LinkedHashMap<>())
                        .computeIfAbsent(q.getTopic().toLowerCase(Locale.ROOT), k -> new ArrayList<>())
                        .add(q);
                loaded++;
            } catch (IllegalArgumentException e) {
                System.err.println("WARNING: skipped CSV row " + (r + 1) + ": " + e.getMessage());
            }
        }
        System.out.println("Loaded " + loaded + " questions.");
    }

    private static Question toQuestion(String[] row, Map<String, Integer> col) {
        String branch = cell(row, col, "branch");
        String topic = cell(row, col, "topic");
        String prompt = cell(row, col, "prompt");
        if (branch.isEmpty() || topic.isEmpty() || prompt.isEmpty()) {
            throw new IllegalArgumentException("branch, topic and prompt must not be empty");
        }

        int difficulty;
        try {
            difficulty = Integer.parseInt(cell(row, col, "difficulty"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("difficulty must be a whole number");
        }
        if (difficulty < 1) {
            throw new IllegalArgumentException("difficulty must be 1 or higher");
        }

        String[] choices = new String[4];
        String[] names = {"choicea", "choiceb", "choicec", "choiced"};
        for (int i = 0; i < 4; i++) {
            choices[i] = cell(row, col, names[i]);
            if (choices[i].isEmpty()) {
                throw new IllegalArgumentException("choice " + (char) ('A' + i) + " is empty");
            }
        }

        String answer = cell(row, col, "answer").toUpperCase(Locale.ROOT);
        if (answer.length() != 1 || answer.charAt(0) < 'A' || answer.charAt(0) > 'D') {
            throw new IllegalArgumentException("answer must be A, B, C or D");
        }

        String hint = cell(row, col, "hint");
        if (hint.isEmpty()) {
            hint = "No hint is available for this question.";
        }
        return new Question(topic, difficulty, prompt, choices, answer.charAt(0) - 'A', hint);
    }

    /** The trimmed cell, or "" when the row is short or the column is absent. Line breaks are kept. */
    private static String cell(String[] row, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        if (i == null || i >= row.length) {
            return "";
        }
        return row[i].replace("\r", "").trim();
    }

    // ------------------------------------------------------------------ queries

    public boolean hasBranch(String branch) {
        return byBranch.containsKey(key(branch));
    }

    /** Topic names of a branch, in the order they first appear in the CSV. */
    public List<String> topics(String branch) {
        List<String> names = new ArrayList<>();
        Map<String, List<Question>> topics = byBranch.get(key(branch));
        if (topics != null) {
            for (List<Question> list : topics.values()) {
                names.add(list.get(0).getTopic());
            }
        }
        return names;
    }

    public int count(String branch, String topic) {
        List<Question> list = topicList(branch, topic);
        return list == null ? 0 : list.size();
    }

    // ------------------------------------------------------------------ drawing rounds

    /** Every question of one topic: shuffled within each difficulty, easiest difficulty first. */
    public Question[] drawTopic(String branch, String topic) {
        return drawTopic(branch, topic, Integer.MAX_VALUE);
    }

    /** Up to limit questions from one topic, freshly shuffled before every round. */
    public Question[] drawTopic(String branch, String topic, int limit) {
        List<Question> list = topicList(branch, topic);
        if (list == null || limit <= 0) {
            return new Question[0];
        }
        List<Question> copy = new ArrayList<>(list);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(limit, copy.size())).toArray(new Question[0]);
    }

    /**
     * A mixed round for a whole branch: up to perDifficulty random questions from each difficulty,
     * pooled across all topics of that branch, easiest difficulty first.
     */
    public Question[] drawBranch(String branch, int perDifficulty) {
        Map<String, List<Question>> topics = byBranch.get(key(branch));
        if (topics == null) {
            return new Question[0];
        }
        List<Question> pool = new ArrayList<>();
        for (List<Question> list : topics.values()) {
            pool.addAll(list);
        }
        return arrange(pool, perDifficulty);
    }

    private Question[] arrange(List<Question> pool, int perDifficulty) {
        Map<Integer, List<Question>> tiers = new TreeMap<>();   // TreeMap keeps difficulties ascending
        for (Question q : pool) {
            tiers.computeIfAbsent(q.getDifficulty(), k -> new ArrayList<>()).add(q);
        }
        List<Question> round = new ArrayList<>();
        for (List<Question> tier : tiers.values()) {
            List<Question> copy = new ArrayList<>(tier);
            Collections.shuffle(copy, random);
            round.addAll(copy.subList(0, Math.min(perDifficulty, copy.size())));
        }
        return round.toArray(new Question[0]);
    }

    private List<Question> topicList(String branch, String topic) {
        Map<String, List<Question>> topics = byBranch.get(key(branch));
        return topics == null ? null : topics.get(key(topic));
    }

    private static String key(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
