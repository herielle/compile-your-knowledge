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
     * loads every question from a csv file and hands out rounds.
     *
     * columns (any order, header row needed):
     * branch, topic, difficulty, prompt, choiceA, choiceB, choiceC, choiceD, answer, hint
     *
     * a round always goes from the lowest difficulty to the highest. inside each difficulty the order
     * is shuffled every time a round is drawn, so no two rounds play out the same way.
     */

public final class QuestionBank {

    public static final String DEFAULT_PATH = "resources/questions/questions.csv";

    private static final String[] REQUIRED = {
        "branch", "topic", "difficulty", "prompt", "choicea", "choiceb", "choicec", "choiced", "answer"
    };

    // branch (lower case) -> topic (lower case) -> questions. both stay in csv order
    private final Map<String, Map<String, List<Question>>> byBranch = new LinkedHashMap<>();
    private final Random random;

    public QuestionBank() {
        this(new Random());
    }

    // pass a seeded Random to get the same shuffles every time (handy for tests)
    public QuestionBank(Random random) {
        this.random = random;
    }

    // ---- loading ----

    // loads the bundled csv. gives an empty bank (and a warning) if it can't be read

    public static QuestionBank loadDefault() {
        QuestionBank bank = new QuestionBank();
        try (Reader reader = open(DEFAULT_PATH)) {
            bank.load(reader);
        } catch (IOException e) {
            System.err.println("WARNING: could not read '" + DEFAULT_PATH + "': " + e.getMessage());
        }
        return bank;
    }

    // opens the csv from the classpath first, then from the src/ folder
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

    // reads questions from any reader. bad rows are reported and skipped, they never crash the game
    public void load(Reader source) throws IOException {
        Reader in = source.markSupported() ? source : new BufferedReader(source);
        List<String[]> records = CsvReader.parse(in);
        
        if (records.isEmpty()) {
            System.err.println("WARNING: the question file is empty.");
            return;
        }

        // find which column each name is in, using the header row
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

    // turns one csv row into a question. throws exception if the row is not valid
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

    // gets a trimmed cell from a row. gives "" if the row is short or the column doesn't exist. line breaks inside are kept
    private static String cell(String[] row, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        if (i == null || i >= row.length) {
            return "";
        }
        return row[i].replace("\r", "").trim();
    }

    // ---- looking things up ----

    // true if there are questions for this branch.
    public boolean hasBranch(String branch) {
        return byBranch.containsKey(key(branch));
    }

    // topic names of a branch, in the order they first show up in the csv.
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

    // how many questions a topic has.
    public int count(String branch, String topic) {
        List<Question> list = topicList(branch, topic);
        return list == null ? 0 : list.size();
    }

    // ---- drawing rounds ----

    // every question of one topic: easiest first, shuffled inside each difficulty.
    public Question[] drawTopic(String branch, String topic) {
        return drawTopic(branch, topic, Integer.MAX_VALUE);
    }

    // same as drawStage. kept so older code that calls drawTopic(branch, topic, limit) still works.
    public Question[] drawTopic(String branch, String topic, int limit) {
        return drawStage(branch, topic, limit);
    }

    /**
     * one stage of the game: up to count questions from a single topic, ordered easy, medium, hard
     * (from the csv difficulty column: 1 = easy, 2 = medium, 3 = hard).
     *
     * randomness only happens inside a difficulty. the tiers are never mixed, so the stage gets
     * harder as it goes.
     *
     * if the topic has more than count questions, each tier gets an even share and the easiest
     * tiers get any leftover (10 questions over 3 tiers = 4 easy, 3 medium, 3 hard). if a tier
     * doesn't have enough, the missing ones come from the nearest tier with some to spare.
     * a topic with count questions or fewer is asked in full. the four choices are shuffled too.
     */

    public Question[] drawStage(String branch, String topic, int count) {
        List<Question> list = topicList(branch, topic);
        if (list == null || count <= 0) {
            return new Question[0];
        }

        List<List<Question>> byTier = new ArrayList<>(groupByDifficulty(list).values());
        int[] quota = tierQuotas(byTier, count);

        List<Question> round = new ArrayList<>();
        for (int t = 0; t < byTier.size(); t++) {
            List<Question> copy = new ArrayList<>(byTier.get(t));
            Collections.shuffle(copy, random);
            for (Question q : copy.subList(0, quota[t])) {
                round.add(q.shuffled(random));
            }
        }
        return round.toArray(new Question[0]);
    }

    // works out how many questions to take from each tier (index 0 = easiest)

    private static int[] tierQuotas(List<List<Question>> byTier, int count) {
        int tiers = byTier.size();
        int[] quota = new int[tiers];
        int total = 0;
        for (List<Question> tier : byTier) {
            total += tier.size();
        }
        if (total <= count) { // small topic: ask everything
            for (int t = 0; t < tiers; t++) {
                quota[t] = byTier.get(t).size();
            }
            return quota;
        }
        for (int t = 0; t < tiers; t++) { // even share, leftovers go to the easiest tiers
            quota[t] = count / tiers + (t < count % tiers ? 1 : 0);
        }
        for (int t = 0; t < tiers; t++) { // move any shortfall to the nearest tier with room
            int missing = quota[t] - byTier.get(t).size();
            if (missing <= 0) {
                continue;
            }
            quota[t] -= missing;
            for (int d = 1; d < tiers && missing > 0; d++) {
                for (int j : new int[]{t + d, t - d}) {
                    if (j >= 0 && j < tiers && missing > 0) {
                        int spare = byTier.get(j).size() - quota[j];
                        int give = Math.min(spare, missing);
                        if (give > 0) {
                            quota[j] += give;
                            missing -= give;
                        }
                    }
                }
            }
        }
        return quota;
    }

    /**
     * a mixed round for a whole branch: up to perDifficulty random questions from each difficulty,
     * taken from all topics of that branch, easiest difficulty first.
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

    // picks up to perDifficulty random questions from each difficulty, easiest first, with shuffled choices
    private Question[] arrange(List<Question> pool, int perDifficulty) {
        List<Question> round = new ArrayList<>();
        for (List<Question> tier : groupByDifficulty(pool).values()) {
            List<Question> copy = new ArrayList<>(tier);
            Collections.shuffle(copy, random);
            for (Question q : copy.subList(0, Math.min(perDifficulty, copy.size()))) {
                round.add(q.shuffled(random));
            }
        }
        return round.toArray(new Question[0]);
    }

    // sorts questions into groups by difficulty. the map keeps difficulties in ascending order
    private static Map<Integer, List<Question>> groupByDifficulty(List<Question> questions) {
        Map<Integer, List<Question>> tiers = new TreeMap<>();
        for (Question q : questions) {
            tiers.computeIfAbsent(q.getDifficulty(), k -> new ArrayList<>()).add(q);
        }
        return tiers;
    }

    // the questions of one topic, or null if the branch or topic doesn't exist
    private List<Question> topicList(String branch, String topic) {
        Map<String, List<Question>> topics = byBranch.get(key(branch));
        return topics == null ? null : topics.get(key(topic));
    }

    // makes a name safe to compare: trimmed and lower case
    private static String key(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
