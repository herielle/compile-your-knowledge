package compileyourknowledge.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The player's volume choices for background music and sound effects (0.0 = off, 1.0 = full).
 * They are remembered between sessions in a small file in the user's home folder.
 */
public final class SoundSettings {

    private static final File FILE =
            new File(System.getProperty("user.home"), ".compileyourknowledge/settings.properties");

    private static volatile float music = 1f;
    private static volatile float effects = 1f;

    static {
        load();
    }

    private SoundSettings() {
    }

    public static float getMusic() {
        return music;
    }

    public static float getEffects() {
        return effects;
    }

    public static void setMusic(float value) {
        music = clamp(value);
    }

    public static void setEffects(float value) {
        effects = clamp(value);
    }

    /**
     * Turns a slider value into the loudness factor that is really applied. Squaring it makes the
     * slider feel even to the ear (50% sounds about half as loud, not almost full volume).
     */
    public static float gain(float sliderValue) {
        return sliderValue * sliderValue;
    }

    public static synchronized void save() {
        Properties props = new Properties();
        props.setProperty("music", String.valueOf(Math.round(music * 100)));
        props.setProperty("effects", String.valueOf(Math.round(effects * 100)));
        try {
            File dir = FILE.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(FILE)) {
                props.store(out, "Compile Your Knowledge - sound settings");
            }
        } catch (IOException e) {
            System.err.println("Could not save sound settings: " + e);
        }
    }

    private static void load() {
        if (!FILE.exists()) {
            return;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(FILE)) {
            props.load(in);
            music = clamp(Integer.parseInt(props.getProperty("music", "100")) / 100f);
            effects = clamp(Integer.parseInt(props.getProperty("effects", "100")) / 100f);
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not read sound settings: " + e);
        }
    }

    private static float clamp(float v) {
        if (Float.isNaN(v)) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, v));
    }
}
