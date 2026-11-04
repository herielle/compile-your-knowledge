package compileyourknowledge.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class ImageLibrary {

    private ImageLibrary() {
    }

    public static BufferedImage load(String path) {

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