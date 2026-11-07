package compileyourknowledge.ui;

import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;


public class ImageButton extends JButton {

    private static final double HOVER_SCALE = 1.10;
    private static final double PRESSED_SCALE = 0.96;
    private static final double EASE = 0.28; // fraction of the remaining distance covered per tick

    private BufferedImage source;   // cropped to the button itself
    private BufferedImage scaled;         // source scaled to the largest size the button can reach (built on first paint)
    private double scaledDensity = 1.0;   // screen density the scaled copy was built for (2.0 on Retina)
    private boolean dimWhenDisabled = false;
    private int baseWidth;
    private int baseHeight;

    private double scale = 1.0;
    private double targetScale = 1.0;
    private final Timer animation;

    public ImageButton(BufferedImage fullCanvasImage, String fallbackText) {
        super(fallbackText);
        this.source = cropToContent(fullCanvasImage);

        setRolloverEnabled(true);
        setFocusable(false); // keeps Space/Enter from re-pressing the last clicked button
        setContentAreaFilled(false);
        
        setBorderPainted(false);
        setFocusPainted(false);
        
        setOpaque(false);
        setMargin(new java.awt.Insets(0, 0, 0, 0));
        
       
            if (source != null) {
                setText(""); // the picture already contains the label
            }

        animation = new Timer(15, e -> step());

        getModel().addChangeListener(e -> {
            if (getModel().isPressed()) {
                setTargetScale(PRESSED_SCALE);
            } else if (getModel().isRollover()) {
                setTargetScale(HOVER_SCALE);
            } else {
                setTargetScale(1.0);
            }
        });
    }


    /** Replaces the visible image while keeping the same button instance and listeners. */
    public void setImage(BufferedImage image) {
        source = cropToContent(image);
        scaled = null;
        if (source != null) {
            setText("");
        }
        repaint();
    }

    /** When true, the button is drawn faded while it is disabled (used for spent lifelines and wrong answers). */
    public void setDimWhenDisabled(boolean dim) {
        dimWhenDisabled = dim;
        repaint();
    }

    /** Height divided by width of the cropped button, used by the layout to size it. */
    public double aspectRatio() {
        return source == null ? 0.28 : (double) source.getHeight() / source.getWidth();
    }

    /** Sets the resting size of the visible button. The component itself should be a bit larger. */
    public void setBaseSize(int width, int height) {
        if (width == baseWidth && height == baseHeight) {
            return;
        }
       
        baseWidth = width;
        baseHeight = height;
        scaled = null; // rebuilt for the new size the next time the button is painted
    }

    private void setTargetScale(double value) {
        targetScale = value;
        if (!animation.isRunning()) {
            animation.start();
        }
    }

    private void step() {
        double diff = targetScale - scale;
       
        if (Math.abs(diff) < 0.002) {
            scale = targetScale;
            animation.stop();
        } else {
            scale += diff * EASE;
        }
        repaint();
    }

   
    @Override
    public boolean contains(int x, int y) {
        double s = Math.max(scale, 1.0);
        double w = baseWidth * s;
        
        double h = baseHeight * s;
        double left = (getWidth() - w) / 2.0;
       
        double top = (getHeight() - h) / 2.0;
        return x >= left && x <= left + w && y >= top && y <= top + h;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (source == null || baseWidth <= 0 || baseHeight <= 0) {
            super.paintComponent(g); // fallback: plain text button if the picture is missing
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        // Scale to real screen pixels, not logical ones, so the button stays sharp on Retina screens.
        double density = Math.max(1.0, g2.getTransform().getScaleX());
        if (scaled == null || density != scaledDensity) {
            scaled = scaleSmooth(source,
                    Math.max(1, (int) Math.round(baseWidth * HOVER_SCALE * density)),
                    Math.max(1, (int) Math.round(baseHeight * HOVER_SCALE * density)));
            scaledDensity = density;
        }
        if (dimWhenDisabled && !isEnabled()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        }
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = (int) Math.round(baseWidth * scale);
        int h = (int) Math.round(baseHeight * scale);
       
        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;
        g2.drawImage(scaled, x, y, w, h, null);
        g2.dispose();
    }

    
    // Cropped versions of images already processed (keyed by the image object itself), so a picture
    // used again, such as a lock button redrawn on every visit, is never re-scanned pixel by pixel.
    private static final java.util.Map<BufferedImage, BufferedImage> CROP_CACHE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** Crops a button picture ahead of time so building the button later is instant. Safe to call from any thread. */
    public static BufferedImage prepare(BufferedImage img) {
        return cropToContent(img);
    }

    private static BufferedImage cropToContent(BufferedImage img) {
        if (img == null) {
            return null;
        }
        BufferedImage cached = CROP_CACHE.get(img);
        if (cached != null) {
            return cached;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w); // one bulk read instead of w*h getRGB calls
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int argb = px[row + x];
                int alpha = (argb >>> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int gr = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                boolean visible = alpha > 10 && (r + gr + b) < 720; // not transparent, not near-white
                if (visible) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        BufferedImage result;
        if (maxX < minX || maxY < minY) {
            result = img;
        } else {
            result = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = result.createGraphics();
            g2.drawImage(img, 0, 0, result.getWidth(), result.getHeight(),
                    minX, minY, maxX + 1, maxY + 1, null);
            g2.dispose();
        }
        CROP_CACHE.put(img, result);
        if (result != img) {
            CROP_CACHE.put(result, result); // cropping an already cropped picture is then instant
        }
        return result;
    }

    private static BufferedImage scaleSmooth(BufferedImage img, int targetW, int targetH) {
        BufferedImage current = img;
        int w = img.getWidth();
        int h = img.getHeight();
        
            while (w / 2 >= targetW && h / 2 >= targetH) {
                w /= 2;
                h /= 2;
                current = resize(current, w, h);
            }

        return resize(current, targetW, targetH);
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
       
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g2.drawImage(img, 0, 0, w, h, null);
        g2.dispose();
        return out;
    }
}