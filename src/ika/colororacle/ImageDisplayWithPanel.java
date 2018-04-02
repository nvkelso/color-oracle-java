/*
 * ImageDisplayWithPanel.java
 *
 * Created on February 22, 2007, 3:16 PM
 *
 */
package ika.colororacle;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * ImageDisplayWithPanel extends ImageDisplay by drawing a second centered
 * raster image ( = the panel) over the image drawn by ImageDisplay. The panel
 * can be interactively moved with the mouse.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ImageDisplayWithPanel extends ImageDisplay
        implements MouseListener, MouseMotionListener {

    /**
     * The raster image to display.
     */
    private Image panel = null;

    /**
     * Vertical position of the top left corner of the panel. The panel will be
     * centered when it is first drawn.
     */
    private int panelTop = -1;

    /**
     * Horizontal position of the top left corner of the panel. The panel will
     * be centered when it is first drawn.
     */
    private int panelLeft = -1;

    /**
     * Horizontal distance between the left border of the panel and the position
     * of the mouse click that started a dragging operation.
     */
    private int dx = 0;

    /**
     * Vertical distance between the left border of the panel and the position
     * of the mouse click that started a dragging operation.
     */
    private int dy = 0;

    /**
     * Flag that is true if the panel is currently being dragged, false
     * otherwise.
     */
    private boolean dragging = false;

    /**
     * A reference to the controller of the application, which is used to hide
     * the main window if the user clicks outside the panel.
     */
    private final ColorOracle colorOracle;

    /**
     * Creates a new instance of ImageDisplayWithPanel
     */
    public ImageDisplayWithPanel(ColorOracle colorOracle) {
        this.colorOracle = colorOracle;
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * Set the image to display.
     */
    public void setPanel(Image panel) {
        this.panel = panel;
        repaint();
    }

    /**
     * Draw the panel image over what ImageDisplay draws.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (panel == null) {
            return;
        }

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

        // make sure the panel is entirely visible. This also initializes 
        // the position of the panel when it is first drawn.
        if (panelTop < 0 || panelLeft < 0) {
            panelLeft = (getWidth() - panel.getWidth(null)) / 2;
            panelTop = (int) ((getHeight() - panel.getHeight(null)) / 2.5);
        }
        g2d.drawImage(panel, panelLeft, panelTop, this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        colorOracle.switchToNormalVision();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (pointOnPanel(e.getPoint())) {
            dx = e.getX() - panelLeft;
            dy = e.getY() - panelTop;
            dragging = true;
        } else {
            dragging = false;
            colorOracle.switchToNormalVision();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragging) {
            dragging = false;
        } else {
            colorOracle.switchToNormalVision();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) {
            return;
        }

        final int panelWidth = panel.getWidth(null);
        final int panelHeight = panel.getHeight(null);

        // remember the old position of the panel
        final int oldPanelLeft = panelLeft;
        final int oldPanelTop = panelTop;

        // compute the new position of the panel and make sure it is entirely
        // visible on screen.
        panelLeft = Math.max(0, e.getX() - dx);
        panelTop = Math.max(0, e.getY() - dy);
        if (panelLeft + panelWidth > getWidth()) {
            panelLeft = getWidth() - panelWidth;
        }
        if (panelTop + panelHeight > getHeight()) {
            panelTop = getHeight() - panelHeight;
        }

        // compute the dirty region: the union of the area previously covered 
        // by the panel and the area covered now.
        final int dirtyX, dirtyY, dirtyWidth, dirtyHeight;
        if (panelLeft > oldPanelLeft) {
            dirtyX = oldPanelLeft;
            dirtyWidth = panelLeft + panelWidth - oldPanelLeft;
        } else {
            dirtyX = panelLeft;
            dirtyWidth = oldPanelLeft + panelWidth - panelLeft;
        }
        if (panelTop > oldPanelTop) {
            dirtyY = oldPanelTop;
            dirtyHeight = panelTop + panelHeight - oldPanelTop;
        } else {
            dirtyY = panelTop;
            dirtyHeight = oldPanelTop + panelHeight - panelTop;
        }

        // only paint the dirty region.
        // paintImmediately is safe to call, since this is running in the 
        // swing event dispatching thread.
        paintImmediately(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    /**
     * Returns true if the passed point is inside the panel, false otherwise.
     */
    private boolean pointOnPanel(Point point) {
        final int w = panel.getWidth(null);
        final int h = panel.getHeight(null);
        if (point.x < panelLeft || point.x > panelLeft + w) {
            return false;
        }
        if (point.y < panelTop || point.y > panelTop + h) {
            return false;
        }
        return true;
    }

}
