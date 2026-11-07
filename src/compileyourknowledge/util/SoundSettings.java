package compileyourknowledge.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * the player's volume for music and sound effects (0.0 = off, 1.0 = full).
 * they are remembered between sessions in a small file in the user's home folder.
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

    // music volume from 0 to 1
    public static float getMusic() {
        return music;
    }

    // sound effects volume from 0 to 1
    public static float getEffects() {
        return effects;
    }

    // sets the music volume (kept between 0 and 1)
    public static void setMusic(float value) {
        music = clamp(value);
    }

    // sets the sound effects volume (kept between 0 and 1)
    public static void setEffects(float value) {
        effects = clamp(value);
    }

    /**
     * turns a slider value into the loudness that is really used. squaring it makes the slider
     * feel even to the ear (50% sounds about half as loud, not almost full).
     */

    public static float gain(float sliderValue) {
        return sliderValue * sliderValue;
    }

    // saves both volumes to the settings file
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

    // reads the volumes from the settings file, if it exists. runs once when the class loads
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

    // keeps a value between 0 and 1. a broken value falls back to full volume
    private static float clamp(float v) {
        if (Float.isNaN(v)) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, v));
    }
}
