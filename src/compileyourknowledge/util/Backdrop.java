package compileyourknowledge.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;

    /**
     * draws a picture so it covers a panel, without resizing it on every repaint.
     *
     * resizing a 3250 x 2298 picture on every repaint (and a button's hover animation repaints every
     * frame) was the main cause of lag. here the picture is resized once per panel size and the result
     * is reused, so drawing it is just a quick copy.
     */

public final class Backdrop {

    private static final int MAX_ENTRIES = 8;

    // identifies one resized copy: which picture and what size
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

    // least recently used first, so old sizes (after a resize or full screen toggle) drop out.
    // the values are soft references, so the jvm can throw these big pictures away instead of
    // running out of memory. a dropped one is just rebuilt when it's needed again.

    private static final Map<Key, SoftReference<BufferedImage>> CACHE =
            new LinkedHashMap<Key, SoftReference<BufferedImage>>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, SoftReference<BufferedImage>> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private Backdrop() {
    }

    // paints the picture so it covers the w x h area (extra edges are cropped equally), using the saved copy. 

    public static void paint(Graphics2D g2, BufferedImage source, int w, int h) {
        if (source == null || w <= 0 || h <= 0) {
            return;
        }

        // on a high density screen (retina) one logical pixel is several real pixels.
        // the copy is built at real size, otherwise it gets stretched and looks soft

        double density = Math.max(1.0, g2.getTransform().getScaleX());
        int pixelsW = (int) Math.round(w * density);
        int pixelsH = (int) Math.round(h * density);
        g2.drawImage(get(source, pixelsW, pixelsH), 0, 0, w, h, null);
    }

    // returns the saved copy for this size, or builds and saves a new one
    private static synchronized BufferedImage get(BufferedImage source, int w, int h) {
        Key key = new Key(source, w, h);
        SoftReference<BufferedImage> ref = CACHE.get(key);
        BufferedImage cached = ref == null ? null : ref.get();
        if (cached == null) {
            cached = build(source, w, h);
            CACHE.put(key, new SoftReference<>(cached));
        }
        return cached;
    }

    // makes a copy of the picture that covers w x h, centred 
    private static BufferedImage build(BufferedImage src, int w, int h) {
        double scale = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
        int drawW = Math.max(w, (int) Math.round(src.getWidth() * scale));
        int drawH = Math.max(h, (int) Math.round(src.getHeight() * scale));

        // shrink in half steps first. it looks much smoother than one big jump
        BufferedImage current = src;
        int cw = src.getWidth();
        int ch = src.getHeight();
        while (cw / 2 >= drawW && ch / 2 >= drawH) {
            cw /= 2;
            ch /= 2;
            current = halve(current, cw, ch);
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(current, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
        g2.dispose();
        return out;
    }

    // makes a smaller copy of a picture at the given size
    private static BufferedImage halve(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(img, 0, 0, w, h, null);
        g2.dispose();
        return out;
    }
}
