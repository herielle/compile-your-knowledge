package compileyourknowledge.util;

import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public final class SoundManager {

    private static final String OPENING_SOUND = "resources/sounds/OpeningGame.mp3";
    private static final String GAMEPLAY_SOUND = "resources/sounds/GamePlay.mp3";

    private static Track opening;
    private static Track gameplay;

    private SoundManager() {
    }

    /** Plays the opening sound once. Restarts it if it is already playing. */
    public static synchronized void playOpening() {
        stopOpening();
        opening = new Track(OPENING_SOUND, false);
        opening.start();
    }

    /** Stops the opening sound and starts the gameplay music, looping forever. */
    public static synchronized void playGameplay() {
        if (gameplay != null && gameplay.isRunning()) {
            return; // already playing, don't stack a second copy on top
        }
        stopOpening();
        gameplay = new Track(GAMEPLAY_SOUND, true);
        gameplay.start();
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
            thread.start();
        }

        boolean isRunning() {
            return thread.isAlive() && !stopped;
        }

        synchronized void stop() {
            stopped = true;
            if (player != null) {
                player.close(); // makes the blocking play() call return
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
                } catch (Exception e) {
                    System.err.println("Sound playback failed for '" + path + "': " + e.getMessage());
                    return;
                }
            } while (loop && !stopped);
        }
    }
}