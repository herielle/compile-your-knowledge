package compileyourknowledge.util;

import javazoom.jl.player.Player;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public final class SoundManager {

    private static final String OPENING_SOUND = "resources/sounds/OpeningGame.mp3";
    private static final String GAMEPLAY_SOUND = "resources/sounds/GamePlay.mp3";

    private static final String CLICK_SOUND = "resources/sounds/Click.mp3";
    private static final String CORRECT_SOUND = "resources/sounds/Correct.mp3";
    private static final String INCORRECT_SOUND = "resources/sounds/Incorrect.mp3";

    private static final java.util.Map<String, byte[]> EFFECT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean clickListenerInstalled = false;

    private static Track opening;
    private static Track gameplay;

    private SoundManager() {
    }

    /**
     * Plays the click sound whenever any enabled button (JButton, ImageButton, GlassButton...) is
     * pressed with the left mouse button. Call once at start-up.
     */
    public static synchronized void installClickSound() {
        if (clickListenerInstalled) {
            return;
        }
        clickListenerInstalled = true;
        preloadEffects();
        Toolkit.getDefaultToolkit().addAWTEventListener((AWTEvent event) -> {
            if (event.getID() == MouseEvent.MOUSE_PRESSED && event.getSource() instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) event.getSource();
                if (b.isEnabled() && SwingUtilities.isLeftMouseButton((MouseEvent) event)) {
                    playClick();
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    public static void playClick() {
        playEffect(CLICK_SOUND);
    }

    public static void playCorrect() {
        playEffect(CORRECT_SOUND);
    }

    public static void playIncorrect() {
        playEffect(INCORRECT_SOUND);
    }

    /** Reads the short effect files into memory on a background thread so the first press is not slow. */
    private static void preloadEffects() {
        Thread t = new Thread(() -> {
            for (String path : new String[]{CLICK_SOUND, CORRECT_SOUND, INCORRECT_SOUND}) {
                effectBytes(path);
            }
        }, "sound-preload");
        t.setDaemon(true);
        t.start();
    }

    private static byte[] effectBytes(String path) {
        byte[] data = EFFECT_CACHE.get(path);
        if (data != null) {
            return data;
        }
        try (InputStream in = open(path)) {
            if (in == null) {
                System.err.println("WARNING: could not load sound '" + path + "'.");
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            data = out.toByteArray();
            EFFECT_CACHE.put(path, data);
            return data;
        } catch (IOException e) {
            System.err.println("Could not read sound '" + path + "': " + e.getMessage());
            return null;
        }
    }

    /** Fire-and-forget: each effect gets its own thread, so effects overlap and never block the UI. */
    private static void playEffect(String path) {
        Thread t = new Thread(() -> {
            byte[] data = effectBytes(path);
            if (data == null) {
                return;
            }
            try {
                new Player(new ByteArrayInputStream(data)).play();
            } catch (Throwable e) {
                System.err.println("Could not play effect '" + path + "': " + e.getMessage());
            }
        }, "effect-" + path);
        t.setDaemon(true);
        t.start();
    }

    /** Plays the opening sound once. Restarts it if it is already playing. */
    public static synchronized void playOpening() {
        try {
            stopOpening();
            opening = new Track(OPENING_SOUND, false);
            opening.start();
        } catch (Throwable t) {
            System.err.println("Could not play opening sound: " + t);
        }
    }

    /** Stops the opening sound and starts the gameplay music, looping forever. */
    public static synchronized void playGameplay() {
        try {
            if (gameplay != null && gameplay.isRunning()) {
                return; // already playing, don't stack a second copy on top
            }
            stopOpening();
            gameplay = new Track(GAMEPLAY_SOUND, true);
            gameplay.start();
        } catch (Throwable t) {
            System.err.println("Could not play gameplay music: " + t);
        }
    }

    public static synchronized void stopOpening() {
        if (opening != null) {
            opening.stop();
            opening = null;
        }
    }

    public static synchronized void stopGameplay() {
        if (gameplay != null) {
            gameplay.stop();
            gameplay = null;
        }
    }

    public static synchronized void stopAll() {
        stopOpening();
        stopGameplay();
    }

    /** Opens the file from the classpath first, then from the src/ folder (same idea as ImageLibrary). */
    private static InputStream open(String path) throws IOException {
        InputStream in = SoundManager.class.getResourceAsStream("/" + path);
        if (in == null) {
            File file = new File("src/" + path);
            if (file.exists()) {
                in = new FileInputStream(file);
            }
        }
        return in == null ? null : new BufferedInputStream(in);
    }

    /** One playing sound on its own thread. */
    private static final class Track implements Runnable {
        private final String path;
        private final boolean loop;
        private final Thread thread;
        private Player player;
        private volatile boolean stopped = false;

        Track(String path, boolean loop) {
            this.path = path;
            this.loop = loop;
            this.thread = new Thread(this, "sound-" + path);
            this.thread.setDaemon(true);
        }

        void start() {
            try {
                thread.start();
            } catch (Throwable t) {
                System.err.println("Sound could not start: " + t);
            }
        }

        boolean isRunning() {
            return thread.isAlive() && !stopped;
        }

        void stop() {
            stopped = true;
            final Player p;
            synchronized (this) {
                p = player;
            }
            if (p != null) {
                // close() can wait for audio to drain, so never do it on the UI thread
                Thread closer = new Thread(() -> {
                    try {
                        p.close(); // makes the blocking play() call return
                    } catch (Throwable ignored) {
                        // nothing to do
                    }
                }, "sound-closer");
                closer.setDaemon(true);
                closer.start();
            }
        }

        private synchronized boolean register(Player p) {
            if (stopped) {
                p.close();
                return false;
            }
            player = p;
            return true;
        }

        @Override
        public void run() {
            do {
                try (InputStream in = open(path)) {
                    if (in == null) {
                        System.err.println("WARNING: could not load sound '" + path
                                + "'. Check the file name and the folder.");
                        return;
                    }
                    Player p = new Player(in);
                    if (!register(p)) {
                        return;
                    }
                    p.play(); // blocks until the track ends or stop() is called
                } catch (Throwable e) {
                    System.err.println("Sound playback failed for '" + path + "': " + e.getMessage());
                    return;
                }
            } while (loop && !stopped);
        }
    }
}