package compileyourknowledge.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class ImageLibrary {

    private ImageLibrary() {
    }

    /**
     * Decoded images, kept so each file is read from disk only once. Before this, every screen and
     * every new question window decoded the big background (3250x2298) again and kept its own copy,
     * which is what made the game slow down the longer it ran. Treat returned images as read-only.
     */
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    public static BufferedImage load(String path) {
        synchronized (CACHE) {
            if (CACHE.containsKey(path)) {
                return CACHE.get(path);
            }
        }
        BufferedImage image = loadUncached(path);   // decoded outside the lock so several can load at once
        synchronized (CACHE) {
            if (CACHE.containsKey(path)) {
                return CACHE.get(path);
            }
            CACHE.put(path, image); // a missing file is remembered too, so the warning prints once
        }
        return image;
    }

    private static BufferedImage loadUncached(String path) {

        // try the original path first
        BufferedImage image = loadImage(path);
        if (image != null) {
            return image;
        }

        // if the image is requested directly from images/,
        // also try the screens folder
        String screenPath = path.replace(
                "resources/images/",
                "resources/images/screens/"
        );

        if (!screenPath.equals(path)) {
            image = loadImage(screenPath);
            if (image != null) {
                return image;
            }
        }

        System.err.println("WARNING: could not load image '" + path
                + "'. Check the file name, the folder, and that it is a PNG or JPG.");

        return null;
    }

    private static BufferedImage loadImage(String path) {

        // try loading from the classpath
        try (InputStream in =
                     ImageLibrary.class.getResourceAsStream("/" + path)) {

            if (in != null) {
                BufferedImage image = ImageIO.read(in);

                if (image != null) {
                    return image;
                }
            }

        } catch (IOException e) {
            System.err.println("Classpath image read failed: " + e.getMessage());
        }

        // try loading directly from src/
        File file = new File("src/" + path);

        if (file.exists()) {
            try {
                BufferedImage image = ImageIO.read(file);

                if (image != null) {
                    return image;
                }

            } catch (IOException e) {
                System.err.println("File image read failed: " + e.getMessage());
            }
        }

        return null;
    }
}