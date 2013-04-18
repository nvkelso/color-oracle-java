package ika.colororacle;

import com.muchsoft.util.Sys;
import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

final class Screen {

    /*
    public static void main(String[] args) {
        
        Screen.detectScreens();

        // simulate color-impaired vision for all attached screens
        for (Screen screen : Screen.getScreens()) {
            try {
                //System.out.println();
                //System.out.println("Screen " + screen.toString());

                // don't take a screenshot when a color-impaired simulation
                // is currently visible. Instead, use the same screenshot again.
                screen.takeScreenshot();
                
                javax.imageio.ImageIO.write(screen.screenshotImage, "png", new java.io.File("/Volumes/Macintosh HD/Users/jenny/Desktop/screenshot" + Screen.getScreens().indexOf(screen) + ".png"));
            } catch (IOException ex) {
                Logger.getLogger(Screen.class.getName()).log(Level.SEVERE, null, ex);
            } catch (AWTException ex) {
                Logger.getLogger(Screen.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    */

    /**
     * An array with all attached screens.
     */
    private static final ArrayList<Screen> screens = new ArrayList<Screen>();

    public static ArrayList<Screen> getScreens() {
        return screens;
    }
    
    public static void detectScreens() {

        // remove previous screens
        Screen.screens.clear();
        
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        
        // multiple monitors are only supported by Color Oracle on Windows systems.
        // Linux systems are not reliable. Mac is inconsitent between versions
        // for Robot.createScreenCapture().
        if (Sys.isWindows()) {
            GraphicsDevice[] gs = ge.getScreenDevices();
            for (GraphicsDevice gd : gs) {
                Screen.screens.add(new Screen(gd.getDefaultConfiguration()));
            }
        } else {
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            Screen.screens.add(new Screen(gd.getDefaultConfiguration()));
        }
        
    }
    
    public MainWindow simulationWindow = null;
    public BufferedImage screenshotImage = null;
    public GraphicsConfiguration gc = null;
    
    private Screen(GraphicsConfiguration gc) {
        super();
        this.gc = gc;
    }

    public Image getSimulationImage() {
        if (simulationWindow == null) {
            return null;
        }
        return simulationWindow.getImage();
    }

    private void createSimulationWindow(ColorOracle colorOracle) {
        this.simulationWindow = new MainWindow(null, false, colorOracle);

        // add event listeners that will hide the window when a key is pressed,
        // or when the window looses focus.
        // Note: There is a bug in Java 6 with listeners for mouse wheel events:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6480024
        // Therefore the mouse wheel event listener is attached to a child of
        // the MainWindow (the ContentPane).
        // Note: mouse events are handled by ImageDisplayWithPanel
        this.simulationWindow.addKeyListener(colorOracle);
        this.simulationWindow.addWindowListener(colorOracle);
        this.simulationWindow.addFocusListener(colorOracle);
        this.simulationWindow.getContentPane().addMouseWheelListener(colorOracle);

        //System.out.println("Window: " + this.getUsableScreenArea());

        // set size and position of the window
        this.simulationWindow.setBounds(this.getUsableScreenArea());
        // need to validate to fix bug in AWT, see
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4919042
        this.simulationWindow.validate();
    }

    private Rectangle getUsableScreenArea() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit == null) {
            return null;
        }
        Insets screenInsets = toolkit.getScreenInsets(gc);
        Rectangle screenRect = gc.getBounds();
        screenRect.x += screenInsets.left;
        screenRect.y += screenInsets.top;
        screenRect.width -= screenInsets.left + screenInsets.right;
        screenRect.height -= screenInsets.top + screenInsets.bottom;
        return screenRect;
    }

    public void takeScreenshot() throws AWTException {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit == null) {
            return;
        }
        Rectangle screenRect = gc.getBounds();

        Insets screenInsets = toolkit.getScreenInsets(gc);
        screenRect.x = screenInsets.left;
        screenRect.y = screenInsets.top;
        screenRect.width -= screenInsets.left + screenInsets.right;
        screenRect.height -= screenInsets.top + screenInsets.bottom;

        Robot robot = new Robot(this.gc.getDevice());
        this.screenshotImage = robot.createScreenCapture(screenRect);
    }

    public void showSimulationImage(BufferedImage simulationImage, 
            ColorOracle colorOracle, Image panel) {

        // don't create a window if there is already one visible
        if (this.simulationWindow == null) {
            this.createSimulationWindow(colorOracle);
        }
        this.simulationWindow.setImage(simulationImage);
        this.simulationWindow.setPanel(panel);

        // Bring our application to the foreground. This discussion is for
        // frames, not dialogs:
        // http://forum.java.sun.com/thread.jspa?threadID=640210&messageID=3762680
        this.simulationWindow.setVisible(true);
        this.simulationWindow.requestFocus();
        this.simulationWindow.toFront();
    }

    /**
     * Hides the simulation window and deallocates the window and the
     * screenshot image.
     */
    public void hideSimulation() {

        if (this.simulationWindow != null) {
            this.simulationWindow.setVisible(false);
            this.simulationWindow.dispose();
            this.simulationWindow = null;
        }
        this.screenshotImage = null;
    }
}
