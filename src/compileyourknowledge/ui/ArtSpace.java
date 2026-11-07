package compileyourknowledge.ui;

import java.awt.Component;

/**
 * turns positions in the artwork (2296 x 1624 units) into real pixels on the panel
 * the artwork is scaled to cover the panel and centred just like the background.
 */

final class ArtSpace {

    static final double WIDTH = 2296;
    static final double HEIGHT = 1624;

    private final Component owner;

    ArtSpace(Component owner) {
        this.owner = owner;
    }

    // how many pixels one artwork unit is worth right now. 
    double scale() {
        return Math.max(owner.getWidth() / WIDTH, owner.getHeight() / HEIGHT);
    }

    double offsetX() {
        return (owner.getWidth() - WIDTH * scale()) / 2.0;
    }

    double offsetY() {
        return (owner.getHeight() - HEIGHT * scale()) / 2.0;
    }

    int sx(double artX) {
        return (int) Math.round(offsetX() + artX * scale());
    }

    int sy(double artY) {
        return (int) Math.round(offsetY() + artY * scale());
    }

    int sl(double artLength) {
        return (int) Math.round(artLength * scale());
    }
}
