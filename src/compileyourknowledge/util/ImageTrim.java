package compileyourknowledge.util;

import java.awt.image.BufferedImage;

public final class ImageTrim {

    private ImageTrim() {
    }

    /** Trims transparent borders so the picture fills the box it is drawn into. */
    public static BufferedImage cropToAlpha(BufferedImage img) {
        if (img == null) {
            return null;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w); // one bulk read instead of w*h getRGB calls
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (((px[y * w + x] >>> 24) & 0xFF) > 10) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return img;
        }
        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
