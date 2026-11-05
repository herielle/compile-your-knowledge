package compileyourknowledge.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws a picture so it covers a panel, without rescaling it on every repaint.
 *
 * Scaling a 3250x2298 picture with bicubic filtering on every repaint (and a repaint happens on
 * every frame of a button's hover animation) was the main cause of the lag. Here the picture is
 * scaled once per panel size and the result is reused; drawing it is then a plain fast copy.
 */
public final class Backdrop {

    private static final int MAX_ENTRIES = 8;

    private static final class Key {
        final BufferedImage source;
        final int w, h;

        Key(BufferedImage source, int w, int h) {
            this.source = source;
            this.w = w;
            this.h = h;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) {
                return false;
            }
            Key k = (Key) o;
            return k.source == source && k.w == w && k.h == h;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(source) * 31 * 31 + w * 31 + h;
        }
    }

    // Least recently used first; old sizes (after a resize or full-screen toggle) fall out of the map.
    private static final Map<Key, BufferedImage> CACHE = new LinkedHashMap<Key, BufferedImage>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private Backdrop() {
    }

    /** Paints source so it covers the w x h area (cropping the overflow equally), using the cached copy. */
    public static void paint(Graphics2D g2, BufferedImage source, int w, int h) {
        if (source == null || w <= 0 || h <= 0) {
            return;
        }
        g2.drawImage(get(source, w, h), 0, 0, null);
    }

    private static synchronized BufferedImage get(BufferedImage source, int w, int h) {
        Key key = new Key(source, w, h);
        BufferedImage cached = CACHE.get(key);
        if (cached == null) {
            cached = build(source, w, h);
            CACHE.put(key, cached);
        }
        return cached;
    }

    private static BufferedImage build(BufferedImage src, int w, int h) {
        double scale = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
        int drawW = Math.max(w, (int) Math.round(src.getWidth() * scale));
        int drawH = Math.max(h, (int) Math.round(src.getHeight() * scale));

        // Halve step by step first: much smoother than one big jump when shrinking a lot.
        BufferedImage current = src;
        int cw = src.getWidth();
        int ch = src.getHeight();
        while (cw / 2 >= drawW && ch / 2 >= drawH) {
            cw /= 2;
            ch /= 2;
            current = draw(current, cw, ch, 0, 0, cw, ch);
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(current, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
        g2.dispose();
        return out;
    }

    private static BufferedImage draw(BufferedImage img, int w, int h, int x, int y, int dw, int dh) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(img, x, y, dw, dh, null);
        g2.dispose();
        return out;
    }
}
