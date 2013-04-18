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
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ImageDisplayWithPanel extends ImageDisplay 
        implements MouseListener, MouseMotionListener {
    
    /**
     * The raster image to display.
     */
    private Image panel = null;
    
    /**
     * Vertical position of the top left corner of the panel.
     * The panel will be centered when it is first drawn.
     */
    private int panelTop = -1;
    
    /**
     * Horizontal position of the top left corner of the panel.
     * The panel will be centered when it is first drawn.
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
     * Flag that is true if the panel is currently being dragged, false otherwise.
     */
    private boolean dragging = false;
    
    /**
     * A reference to the controller of the application, which is used to hide
     * the main window if the user clicks outside the panel.
     */
    private ColorOracle colorOracle;
    
    /** Creates a new instance of ImageDisplayWithPanel */
    public ImageDisplayWithPanel(ColorOracle colorOracle) {
        this.colorOracle = colorOracle;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }
    
    /**
     * Set the image to display.
     */
    public void setPanel (Image panel) {
        this.panel = panel;
        this.repaint();
    }
    
    /**
     * Draw the panel image over what ImageDisplay draws.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        
        if (this.panel == null) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D)g;
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
        if (this.panelTop < 0 || this.panelLeft < 0) {
            this.panelLeft = (this.getWidth() - this.panel.getWidth(null)) / 2;
            this.panelTop = (int)((this.getHeight() - this.panel.getHeight(null)) / 2.5);
        }
        g2d.drawImage(panel, panelLeft, panelTop, this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        this.colorOracle.switchToNormalVision();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (this.pointOnPanel(e.getPoint())) {
            this.dx = e.getX() - this.panelLeft;
            this.dy = e.getY() - this.panelTop;
            this.dragging = true;
        } else {
            this.dragging = false;
            this.colorOracle.switchToNormalVision();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (this.dragging) {
            this.dragging = false;
        } else {
            this.colorOracle.switchToNormalVision();
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
        if (!this.dragging) {
            return;
        }
        
        final int panelWidth = this.panel.getWidth(null);
        final int panelHeight = this.panel.getHeight(null);
        
        // remember the old position of the panel
        final int oldPanelLeft = this.panelLeft;
        final int oldPanelTop = this.panelTop;

        // compute the new position of the panel and make sure it is entirely
        // visible on screen.
        this.panelLeft = Math.max(0, e.getX() - this.dx);
        this.panelTop = Math.max(0, e.getY() - this.dy);
        if (this.panelLeft + panelWidth > this.getWidth())
            this.panelLeft = this.getWidth() - panelWidth;
        if (this.panelTop + panelHeight > this.getHeight())
            this.panelTop = this.getHeight() - panelHeight;
        
        // compute the dirty region: the union of the area previously covered 
        // by the panel and the area covered now.
        final int dirtyX, dirtyY, dirtyWidth, dirtyHeight;
        if (this.panelLeft > oldPanelLeft) {
            dirtyX = oldPanelLeft;
            dirtyWidth = this.panelLeft + panelWidth - oldPanelLeft;
        } else {
            dirtyX = this.panelLeft;
            dirtyWidth = oldPanelLeft + panelWidth - this.panelLeft;
        }
        if (this.panelTop > oldPanelTop) {
            dirtyY = oldPanelTop;
            dirtyHeight = this.panelTop + panelHeight - oldPanelTop;
        } else {
            dirtyY = this.panelTop;
            dirtyHeight = oldPanelTop + panelHeight - this.panelTop;
        }
        
        // only paint the dirty region.
        // paintImmediately is safe to call, since this is running in the 
        // swing event dispatching thread.
        this.paintImmediately(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
    }

    public void mouseMoved(MouseEvent e) {
    }

    /**
     * Returns true if the passed point is inside the panel, false otherwise.
     */
    private boolean pointOnPanel(Point point) {
        final int w = this.panel.getWidth(null);
        final int h = this.panel.getHeight(null);
        if (point.x < this.panelLeft || point.x >  this.panelLeft + w)
            return false;
        if (point.y <  this.panelTop || point.y > this.panelTop + h)
            return false;
        return true;
    }
    
}
