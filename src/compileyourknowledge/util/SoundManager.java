package compileyourknowledge.util;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.Player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
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

    /** Raw mp3 bytes of the short effects (only used as a fallback while the fast version is loading). */
    private static final java.util.Map<String, byte[]> EFFECT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    /** Effects that are fully decoded and ready to play with no start-up cost. */
    private static final java.util.Map<String, Effect> EFFECTS = new java.util.concurrent.ConcurrentHashMap<>();
    /** Quiet samples at the very start of a decoded effect are cut off (16-bit sample value). */
    private static final int SILENCE_THRESHOLD = 400;
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

    /**
     * Decodes the short effects once, on a background thread, and opens their audio lines ahead of
     * time. Playing them later only has to press "start" on a line that is already open, which is
     * what removes the delay after a click.
     */
    private static void preloadEffects() {
        Thread t = new Thread(() -> {
            for (String path : new String[]{CLICK_SOUND, CORRECT_SOUND, INCORRECT_SOUND}) {
                Effect effect = loadEffect(path);
                if (effect != null) {
                    EFFECTS.put(path, effect);
                }
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

    /** Plays a short effect. Uses the ready-made line when there is one, so it starts at once. */
    private static void playEffect(String path) {
        if (SoundSettings.getEffects() <= 0f) {
            return; // effects are turned off
        }
        Effect effect = EFFECTS.get(path);
        if (effect != null) {
            effect.play();
            return;
        }
        playEffectStreaming(path); // the preload has not finished yet: slower, but still works
    }

    /** Fallback: decodes and plays on its own thread, so it never blocks the UI (but it starts late). */
    private static void playEffectStreaming(String path) {
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

    /** Decodes the mp3 to raw sound and opens a few lines for it, so overlapping plays are possible. */
    private static Effect loadEffect(String path) {
        byte[] mp3 = effectBytes(path);
        if (mp3 == null) {
            return null;
        }
        try {
            Pcm pcm = decode(mp3);
            AudioFormat format = new AudioFormat(pcm.sampleRate, 16, pcm.channels, true, false);
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip[] clips = new Clip[Effect.VOICES];
            for (int i = 0; i < clips.length; i++) {
                clips[i] = (Clip) AudioSystem.getLine(info);
                clips[i].open(format, pcm.data, 0, pcm.data.length);
            }
            return new Effect(clips);
        } catch (Throwable e) {
            System.err.println("Could not prepare effect '" + path + "': " + e);
            return null;
        }
    }

    private static Pcm decode(byte[] mp3) throws Exception {
        Bitstream bitstream = new Bitstream(new ByteArrayInputStream(mp3));
        Decoder decoder = new Decoder();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int rate = 44100;
        int channels = 2;
        try {
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                rate = samples.getSampleFrequency();
                channels = samples.getChannelCount();
                short[] buf = samples.getBuffer();
                int len = samples.getBufferLength();
                for (int i = 0; i < len; i++) {
                    out.write(buf[i] & 0xFF);          // little endian, signed 16-bit
                    out.write((buf[i] >> 8) & 0xFF);
                }
                bitstream.closeFrame();
            }
        } finally {
            bitstream.close();
        }
        return new Pcm(trimLeadingSilence(out.toByteArray(), channels), rate, channels);
    }

    /** mp3 files start with a short stretch of silence; cutting it makes the click feel instant. */
    private static byte[] trimLeadingSilence(byte[] data, int channels) {
        int frameBytes = 2 * channels;
        int start = 0;
        while (start + 1 < data.length) {
            int sample = (short) ((data[start] & 0xFF) | (data[start + 1] << 8));
            if (Math.abs(sample) > SILENCE_THRESHOLD) {
                break;
            }
            start += 2;
        }
        start -= start % frameBytes;
        if (start >= data.length) {
            start = 0; // all quiet: keep it as it is
        }
        int length = data.length - start;
        length -= length % frameBytes;
        return java.util.Arrays.copyOfRange(data, start, start + length);
    }

    private static final class Pcm {
        final byte[] data;
        final int sampleRate;
        final int channels;

        Pcm(byte[] data, int sampleRate, int channels) {
            this.data = data;
            this.sampleRate = sampleRate;
            this.channels = channels;
        }
    }

    /** A short sound held in a few already-open lines, so it can play again before it has finished. */
    private static final class Effect {
        static final int VOICES = 3;
        private final Clip[] clips;
        private int next = 0;

        Effect(Clip[] clips) {
            this.clips = clips;
        }

        synchronized void play() {
            for (Clip c : clips) {
                if (!c.isRunning()) {
                    restart(c);
                    return;
                }
            }
            restart(clips[next]); // every line is busy: cut the oldest one off
            next = (next + 1) % clips.length;
        }

        private void restart(Clip c) {
            c.stop();
            applyVolume(c);
            c.setFramePosition(0);
            c.start();
        }

        /** Sets the line's gain from the sound-effects slider just before it plays. */
        private void applyVolume(Clip c) {
            if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                double factor = Math.max(SoundSettings.gain(SoundSettings.getEffects()), 0.0001f);
                float db = (float) (20.0 * Math.log10(factor));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            }
        }
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

    /**
     * One music track on its own thread. It is decoded here instead of by JLayer's Player so the
     * volume slider can change the loudness while the music is playing.
     */
    private static final class Track implements Runnable {
        private static final double BUFFER_SECONDS = 0.2; // also how quickly a volume change is heard

        private final String path;
        private final boolean loop;
        private final Thread thread;
        private volatile boolean stopped = false;
        private volatile SourceDataLine line;

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
            final SourceDataLine l = line;
            if (l != null) {
                // closing can wait for audio to drain, so never do it on the UI thread
                Thread closer = new Thread(() -> {
                    try {
                        l.stop();
                        l.flush();
                        l.close(); // makes a blocked write() return
                    } catch (Throwable ignored) {
                        // nothing to do
                    }
                }, "sound-closer");
                closer.setDaemon(true);
                closer.start();
            }
        }

        @Override
        public void run() {
            try {
                do {
                    try (InputStream in = open(path)) {
                        if (in == null) {
                            System.err.println("WARNING: could not load sound '" + path
                                    + "'. Check the file name and the folder.");
                            return;
                        }
                        playStream(in);
                    }
                } while (loop && !stopped);
                SourceDataLine l = line;
                if (!stopped && l != null) {
                    l.drain(); // let the end of a one-off sound finish
                }
            } catch (Throwable e) {
                if (!stopped) {
                    System.err.println("Sound playback failed for '" + path + "': " + e.getMessage());
                }
            } finally {
                closeLine();
            }
        }

        /** Decodes frame by frame, scales each one by the current music volume and writes it out. */
        private void playStream(InputStream in) throws Exception {
            Bitstream bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();
            byte[] out = new byte[0];
            try {
                Header header;
                while (!stopped && (header = bitstream.readFrame()) != null) {
                    SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    SourceDataLine l = line;
                    if (l == null) {
                        l = openLine(samples.getSampleFrequency(), samples.getChannelCount());
                    }
                    short[] buf = samples.getBuffer();
                    int len = samples.getBufferLength();
                    if (out.length < len * 2) {
                        out = new byte[len * 2];
                    }
                    float gain = SoundSettings.gain(SoundSettings.getMusic());
                    for (int i = 0; i < len; i++) {
                        int v = Math.round(buf[i] * gain);
                        out[2 * i] = (byte) v;            // little endian, signed 16-bit
                        out[2 * i + 1] = (byte) (v >> 8);
                    }
                    l.write(out, 0, len * 2);
                    bitstream.closeFrame();
                }
            } finally {
                bitstream.close();
            }
        }

        private SourceDataLine openLine(int rate, int channels) throws Exception {
            AudioFormat format = new AudioFormat(rate, 16, channels, true, false);
            SourceDataLine l = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
            int frameBytes = 2 * channels;
            int buffer = (int) (rate * BUFFER_SECONDS) * frameBytes;
            l.open(format, buffer);
            l.start();
            line = l;
            return l;
        }

        private void closeLine() {
            SourceDataLine l = line;
            if (l != null) {
                try {
                    l.stop();
                    l.close();
                } catch (Throwable ignored) {
                    // nothing to do
                }
            }
        }
    }
}
