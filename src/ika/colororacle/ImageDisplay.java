/*
 * ImageDisplay.java
 *
 * Created on September 29, 2006, 10:01 AM
 *
 */
package ika.colororacle;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * A Swing component that displays an image.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ImageDisplay extends JComponent {

    /**
     * The image to display.
     */
    private Image image;

    /**
     * Creates a new instance of ImageDisplay
     */
    public ImageDisplay() {
    }

    /**
     * Set the image to display.
     *
     * @param image The new image to display.
     */
    public void setImage(Image image) {
        this.image = image;
        repaint();
    }

    /**
     * Returns the image displayed by this component.
     *
     * @return The image.
     */
    public Image getImage() {
        return image;
    }

    /**
     * Paints the image in the top-left corner of the component.
     */
    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING,
                RenderingHints.VALUE_DITHER_DISABLE);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);

        g2d.drawImage(image, 0, 0, this);
    }

    /**
     * Returns the preferred size which is equal to the size of the image that
     * is displayed.
     */
    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(image.getWidth(null), image.getHeight(null));
        } else {
            return new Dimension(100, 100);
        }
    }

    /**
     * Inform Swing that this JComponent is opaque, i.e. we are drawing the
     * whole area of this Component. This accelerates the drawing of the
     * component.
     *
     * @return Always true.
     */
    @Override
    public boolean isOpaque() {
        return true;
    }

}
