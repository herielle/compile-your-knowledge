package compileyourknowledge.ui;

import java.awt.Component;

/**
 * Converts positions in the artwork (the mockup, 2296 x 1624 units) into pixels of the panel that
 * shows it. The artwork is scaled to cover the panel and centred, exactly like the background.
 */
final class ArtSpace {

    static final double WIDTH = 2296;
    static final double HEIGHT = 1624;

    private final Component owner;

    ArtSpace(Component owner) {
        this.owner = owner;
    }

    double scale() {
        return Math.max(owner.getWidth() / WIDTH, owner.getHeight() / HEIGHT);
    }

    double offsetX() {
        return (owner.getWidth() - WIDTH * scale()) / 2.0;
    }

    double offsetY() {
        return (owner.getHeight() - HEIGHT * scale()) / 2.0;
    }

    /** Artwork x to pixel x. */
    int sx(double artX) {
        return (int) Math.round(offsetX() + artX * scale());
    }

    /** Artwork y to pixel y. */
    int sy(double artY) {
        return (int) Math.round(offsetY() + artY * scale());
    }

    /** Artwork length to pixel length. */
    int sl(double artLength) {
        return (int) Math.round(artLength * scale());
    }
}
