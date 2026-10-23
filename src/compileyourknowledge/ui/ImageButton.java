package compileyourknowledge.ui;

import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;


public class ImageButton extends JButton {

    private static final double HOVER_SCALE = 1.10;
    private static final double PRESSED_SCALE = 0.96;
    private static final double EASE = 0.28; // fraction of the remaining distance covered per tick

    private final BufferedImage source;   // cropped to the button itself
    private BufferedImage scaled;         // source scaled to the largest size the button can reach
    private int baseWidth;
    private int baseHeight;

    private double scale = 1.0;
    private double targetScale = 1.0;
    private final Timer animation;

    public ImageButton(BufferedImage fullCanvasImage, String fallbackText) {
        super(fallbackText);
        this.source = cropToContent(fullCanvasImage);

        setRolloverEnabled(true);
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

        if (source != null && width > 0 && height > 0) {
            scaled = scaleSmooth(source, (int) Math.round(width * HOVER_SCALE),
                    (int) Math.round(height * HOVER_SCALE));
        }
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
        if (scaled == null) {
            super.paintComponent(g); // fallback: plain text button if the picture is missing
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
       
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = (int) Math.round(baseWidth * scale);
        int h = (int) Math.round(baseHeight * scale);
       
        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;
        g2.drawImage(scaled, x, y, w, h, null);
        g2.dispose();
    }

    
    private static BufferedImage cropToContent(BufferedImage img) {
        if (img == null) {
            return null;
        }
        int minX = img.getWidth();
        int minY = img.getHeight();
        
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
               
                int r = (argb >> 16) & 0xFF;
                int gr = (argb >> 8) & 0xFF;
               
                int b = argb & 0xFF;
                boolean visible = alpha > 10 && (r + gr + b) < 720; // not transparent, not near-white
                if (visible) {
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
        BufferedImage out = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
       
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(img, 0, 0, out.getWidth(), out.getHeight(),
                minX, minY, maxX + 1, maxY + 1, null);
        
                g2.dispose();
        return out;
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