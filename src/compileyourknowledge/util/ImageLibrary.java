package compileyourknowledge.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

     /** loads pictures and remembers them, so each file is only read from disk once. */

public final class ImageLibrary {

    // pictures already loaded. before this, every screen decoded the big background again and kept
    // its own copy, which made the game slower the longer it ran. treat returned images as read-only.

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private ImageLibrary() {
    }

    // gets a picture by path (loads it the first time). returns null if the file can't be found 

    public static BufferedImage load(String path) {
        synchronized (CACHE) {
            if (CACHE.containsKey(path)) {
                return CACHE.get(path);
            }
        }
        BufferedImage image = loadUncached(path); // decoded outside the lock so several can load at once
        synchronized (CACHE) {
            if (CACHE.containsKey(path)) {
                return CACHE.get(path);
            }
            CACHE.put(path, image); // a missing file is remembered too, so the warning only prints once
        }
        return image;
    }

    /**
     * swaps a stored picture for a smaller version of itself (like a button cropped from its huge
     * transparent canvas), so the big original can be cleaned up by the garbage collector
     */

    public static void replace(String path, BufferedImage image) {
        if (image == null) {
            return;
        }
        synchronized (CACHE) {
            CACHE.put(path, image);
        }
    }

    //  tries the given path, then the screens folder. prints a warning if both fail
    private static BufferedImage loadUncached(String path) {
        BufferedImage image = loadImage(path);
        if (image != null) {
            return image;
        }

        // if the picture was asked for straight from images/, also look in the screens folder
        String screenPath = path.replace("resources/images/", "resources/images/screens/");
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

    // looks for the picture on the classpath first, then in the src/ folder. returns null if not found 
    private static BufferedImage loadImage(String path) {
        try (InputStream in = ImageLibrary.class.getResourceAsStream("/" + path)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    return image;
                }
            }
        } catch (IOException e) {
            System.err.println("Classpath image read failed: " + e.getMessage());
        }

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
