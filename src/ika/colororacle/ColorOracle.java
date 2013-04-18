/*
 * ColorOraclerOracle.java
 *
 * Created on February 4, 2007, 10:20 PM
 *
 */
package ika.colororacle;

import com.muchsoft.util.Sys;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * ColorOracle is the main class of the program. It creates the tray icon and
 * handles all events, except for mouse events, which are handeled by 
 * ImageDisplayWithPanel.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ColorOracle
        implements KeyListener, WindowListener, FocusListener, MouseWheelListener {

    /**
     * A tooltip that is displayed when the mouse hover over the tray icon.
     */
    private static final String TOOLTIP = "Simulate Color-impaired Vision with Color Oracle";
    /**
     * An error message that is displayed if the system does not support tray icons.
     */
    private static final String TRAYICONS_NOT_SUPPORTED_MESSAGE =
            "Tray icons are not supported on this system. "
            + "Color Oracle will therefore quit.";
    /**
     * The name of the icon that is placed in the task bar.
     */
    private static final String MENUICON = "menuIcon.gif";
    /**
     * The information panel image for deuteranopia.
     */
    private Image deutanPanel = loadImage("deutanpanel.png");
    /**
     * The information panel image for protanopia.
     */
    private Image protanPanel = loadImage("protanpanel.png");
    /**
     * The information panel image for tritanopia.
     */
    private Image tritanPanel = loadImage("tritanpanel.png");
    /**
     * Wait a few milliseconds before taking a screenshot until the menu has 
     * faded out.
     */
    private static final long SLEEP_BEFORE_SCREENSHOT_MILLISECONDS = 300;

    /**
     * Enumerate the four possible states of the current simulation.
     */
    enum Simulation {

        normal, deutan, protan, tritan
    }
    /**
     * Keep track of the current type of color-impairement simulation.
     */
    private Simulation currentSimulation = Simulation.normal;
    /**
     * A flag that is true when a save-as dialog is open and false otherwise.
     */
    private boolean currentlySavingImage = false;
    /**
     * The about dialog.
     */
    private JDialog aboutDialog = null;
    /**
     * The simulator does the actual simulation work.
     */
    private Simulator simulator = new Simulator();
    /**
     * A menu item for normal vision that will be added to the tray menu.
     */
    private CheckboxMenuItem normalMenuItem = new CheckboxMenuItem();
    /**
     * A menu item for deuteranopia that will be added to the tray menu.
     */
    private CheckboxMenuItem deutanMenuItem = new CheckboxMenuItem();
    /**
     * A menu item for protanopia that will be added to the tray menu.
     */
    private CheckboxMenuItem protanMenuItem = new CheckboxMenuItem();
    /**
     * A menu item for tritanopia that will be added to the tray menu.
     */
    private CheckboxMenuItem tritanMenuItem = new CheckboxMenuItem();
    /**
     * The Save menu item that will be added to the tray menu.
     */
    private MenuItem saveMenuItem = new MenuItem();
    /**
     * The About menu item that will be added to the tray menu.
     */
    private MenuItem aboutMenuItem = new MenuItem();
    private long timeOfLastClickOnTrayIcon = 0;
    private long timeOfLastFocusLost = 0;

    /**
     * Entry point for the Color Oracle application.
     * @param args The standard command line arguments, which are ignored.
     */
    public static void main(String[] args) throws IOException {

        // don't run in headless mode
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Headless mode not supported by Color Oracle.");
            System.exit(-1);
            return;
        }

        // default Look and Feel on some systems is Metal, install the native 
        // look and feel instead.  
        try {
            String nativeLF = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(nativeLF);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // set icon for JOptionPane dialogs, e.g. for error messages.
        ColorOracle.setOptionPaneIcons("/ika/icons/icon48x48.png");

        // make sure screenshots are allowed by the security manager
        try {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkPermission(new AWTPermission("createRobot"));
                security.checkPermission(new AWTPermission("readDisplayPixels"));
            }
        } catch (SecurityException se) {
            se.printStackTrace();
            ColorOracle.showErrorMessage("Screenshots are not possible on "
                    + "your system.", true);
            System.exit(-1);
            return;
        }

        // test whether the system supports the SystemTray
        try {
            if (!SystemTray.isSupported()) {
                throw new UnsupportedOperationException("SystemTray not supported");
            }
        } catch (Exception se) {
            ColorOracle.showErrorMessage("Access to the system tray or "
                    + "notification area \nis not supported on your system.",
                    true);
            se.printStackTrace();
            System.exit(-1);
            return;
        }

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    new ColorOracle();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        });
    }

    /**
     * Changes the icon displayed in JOptionPane dialogs to the passed icon.
     * Error, information, question and warning dialogs will show this icon.
     * This will also replace the icon in ProgressMonitor dialogs.
     */
    public static void setOptionPaneIcons(String iconPath) {
        LookAndFeel lf = UIManager.getLookAndFeel();
        if (lf != null) {
            Class iconBaseClass = lf.getClass();
            Object appIcon = LookAndFeel.makeIcon(iconBaseClass, iconPath);
            UIManager.put("OptionPane.errorIcon", appIcon);
            UIManager.put("OptionPane.informationIcon", appIcon);
            UIManager.put("OptionPane.questionIcon", appIcon);
            UIManager.put("OptionPane.warningIcon", appIcon);
        }
    }

    /**
     * Constructor of Color Oracle. Initializes the tray icon and its menu.
     */
    private ColorOracle() throws Exception {
        this.initTrayIcon();
    }

    /**
     * Loads a raster icon from the /ika/icons/ folder.
     * @param name The name of the icon.
     * @description A description of the icon that is attached to it.
     * @return An ImageIcon.
     **/
    public static ImageIcon loadImageIcon(String name, String description) {

        try {
            String folder = "/ika/icons/";
            java.net.URL imgURL = ColorOracle.class.getResource(folder + name);
            if (imgURL != null) {
                ImageIcon imageIcon = new ImageIcon(imgURL, description);
                if (imageIcon.getIconWidth() == 0 || imageIcon.getIconHeight() == 0) {
                    imageIcon = null;
                }
                return imageIcon;
            } else {
                System.err.println("Couldn't find file: " + name);
                return null;
            }
        } catch (Exception exc) {
            return null;
        }
    }

    /**
     * Loads a raster icon from the /ika/icons/ folder.
     * @param name The name of the icon.
     * @return The icon as Image object.
     **/
    public static Image loadImage(String name) {
        ImageIcon icon = loadImageIcon(name, "");
        return icon.getImage();
    }

    /**
     * Hides the color-impairement simulation.
     */
    private void hideSimulation() {

        for (Screen screen : Screen.getScreens()) {
            screen.hideSimulation();
        }
        Screen.getScreens().clear();

    }

    /**
     * Initializes the tray icon and attaches it to the system tray.
     */
    private void initTrayIcon() throws Exception {

        if (!SystemTray.isSupported()) {
            throw new Exception(TRAYICONS_NOT_SUPPORTED_MESSAGE);
        }

        // get the SystemTray instance
        SystemTray tray = SystemTray.getSystemTray();

        // Create an action listener to listen for default actions executed
        // on the tray icon. On Windows, this is a left double-click on
        // the tray icon.
        ActionListener defaultActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                simulate(ColorOracle.Simulation.deutan);
            }
        };

        // create the menu
        PopupMenu menu = this.initMenu();

        // get the image for the TrayIcon
        ImageIcon icon = loadImageIcon(MENUICON, "Color Oracle Icon");
        Image image = icon.getImage();

        // create the TrayIcon
        TrayIcon trayIcon = new TrayIcon(image, TOOLTIP, menu);
        trayIcon.addActionListener(defaultActionListener);
        trayIcon.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // event handler that remembers the last time the tray icon was clicked.
                long currentTime = System.currentTimeMillis();
                if (currentTime > timeOfLastClickOnTrayIcon) {
                    timeOfLastClickOnTrayIcon = currentTime;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        trayIcon.setImageAutoSize(false);

        // add the TrayIcon to the SystemTray
        tray.add(trayIcon);

    }

    /**
     * Constructs the menu with all items and event handlers.
     * @return The new PopupMenu that can be added to the tray icon.
     */
    private PopupMenu initMenu() {

        // create a menu
        PopupMenu menu = new PopupMenu();
        MenuItem quitMenuItem = new MenuItem();

        menu.setLabel("PopupMenu");

        // normal vision
        normalMenuItem.setLabel("Normal Vision");
        normalMenuItem.setState(true);
        normalMenuItem.addItemListener(new java.awt.event.ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent evt) {
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    switchToNormalVision();
                } else if (currentSimulation == Simulation.normal) {
                    normalMenuItem.setState(true); // this will not trigger another event
                }
            }
        });
        menu.add(normalMenuItem);

        // deutan vision
        menu.addSeparator();
        deutanMenuItem.setLabel("Deuteranopia (Common)");
        deutanMenuItem.addItemListener(new java.awt.event.ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent evt) {
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    simulate(ColorOracle.Simulation.deutan);
                } else if (currentSimulation == Simulation.deutan) {
                    deutanMenuItem.setState(true); // this will not trigger another event
                }
            }
        });
        menu.add(deutanMenuItem);

        // protan vision
        protanMenuItem.setLabel("Protanopia (Rare)");
        protanMenuItem.addItemListener(new java.awt.event.ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent evt) {
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    simulate(ColorOracle.Simulation.protan);
                } else if (currentSimulation == Simulation.protan) {
                    protanMenuItem.setState(true); // this will not trigger another event
                }
            }
        });
        menu.add(protanMenuItem);

        // tritan vision
        tritanMenuItem.setLabel("Tritanopia (Very Rare)");
        tritanMenuItem.addItemListener(new java.awt.event.ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent evt) {
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    simulate(ColorOracle.Simulation.tritan);
                } else if (currentSimulation == Simulation.tritan) {
                    tritanMenuItem.setState(true); // this will not trigger another event
                }
            }
        });
        menu.add(tritanMenuItem);

        menu.addSeparator();
        /*
        // save image
        saveMenuItem.setLabel("Save Filtered Screen Image...");
        saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
        
        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveMenuItemActionPerformed(evt);
        }
        });
        menu.add(saveMenuItem);
         */
        // about
        aboutMenuItem.setLabel("About...");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        menu.add(aboutMenuItem);

        menu.addSeparator();

        // exit
        quitMenuItem.setLabel("Exit Color Oracle");
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                System.exit(0);
            }
        });
        menu.add(quitMenuItem);

        return menu;

    }

    /**
     * Returns the name of the current simulation.
     */
    private String currentSimulationName() {
        switch (this.currentSimulation) {
            case deutan:
                return "Deuteranopia";
            case protan:
                return "Protanopia";
            case tritan:
                return "Tritanopia";
            default:
                return "";
        }
    }

    private void rgb2lab(int R, int G, int B, int[] lab) {
        //http://www.brucelindbloom.com

        float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
        float Ls, as, bs;
        float eps = 216.f / 24389.f;
        float k = 24389.f / 27.f;

        float Xr = 0.964221f;  // reference white D50
        float Yr = 1.0f;
        float Zr = 0.825211f;

        // RGB to XYZ
        r = R / 255.f; //R 0..1
        g = G / 255.f; //G 0..1
        b = B / 255.f; //B 0..1

        // assuming sRGB (D65)
        if (r <= 0.04045) {
            r = r / 12;
        } else {
            r = (float) Math.pow((r + 0.055) / 1.055, 2.4);
        }

        if (g <= 0.04045) {
            g = g / 12;
        } else {
            g = (float) Math.pow((g + 0.055) / 1.055, 2.4);
        }

        if (b <= 0.04045) {
            b = b / 12;
        } else {
            b = (float) Math.pow((b + 0.055) / 1.055, 2.4);
        }


        X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
        Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
        Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

        // XYZ to Lab
        xr = X / Xr;
        yr = Y / Yr;
        zr = Z / Zr;

        if (xr > eps) {
            fx = (float) Math.pow(xr, 1 / 3.);
        } else {
            fx = (float) ((k * xr + 16.) / 116.);
        }

        if (yr > eps) {
            fy = (float) Math.pow(yr, 1 / 3.);
        } else {
            fy = (float) ((k * yr + 16.) / 116.);
        }

        if (zr > eps) {
            fz = (float) Math.pow(zr, 1 / 3.);
        } else {
            fz = (float) ((k * zr + 16.) / 116);
        }

        Ls = (116 * fy) - 16;
        as = 500 * (fx - fy);
        bs = 200 * (fy - fz);

        lab[0] = (int) (2.55 * Ls + .5);
        lab[1] = (int) (as + .5);
        lab[2] = (int) (bs + .5);
    }

    private void rgb2lab(int rgb, int[] lab) {
        int r = (0xff0000 & rgb) >> 16;
        int g = (0xff00 & rgb) >> 8;
        int b = 0xff & rgb;
        rgb2lab(r, g, b, lab);
    }

    private BufferedImage computeDifference(BufferedImage img1, BufferedImage img2) {

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            throw new IllegalArgumentException();
        }

        final int width = img1.getWidth();
        final int height = img1.getHeight();
        BufferedImage difImg = new BufferedImage(width, height, img1.getType());
        int[] lab1 = new int[3];
        int[] lab2 = new int[3];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                final int rgb1 = img1.getRGB(c, r);
                final int rgb2 = img2.getRGB(c, r);
                rgb2lab(rgb1, lab1);
                rgb2lab(rgb2, lab2);

                // better: Delta E http://www.brucelindbloom.com/index.html?Eqn_DeltaE_CMC.html

                final int dL = lab1[0] - lab2[0];
                final int da = lab1[1] - lab2[1];
                final int db = lab1[2] - lab2[2];
                final double dE = Math.sqrt(dL * dL + da * da + db * db);

                // mark large values with color
                final int rgb;
                if (dE > 40) {
                    rgb = 0xff0000ff;
                } else {
                    rgb = rgb2;
                }
                /*
                // convert to bw image
                final int q = (int)(Math.min(dE, 255));
                final int rgb = (int)(q << 16 | q << 8 | q | 0xff000000);
                 */
                /*
                // convert dE to color
                int r1 = (0xff0000 & rgb1) >> 16;
                int g1 = (0xff00 & rgb1) >> 8;
                int b1 = 0xff & rgb1;
                
                final int dEMax = 50;
                final int dEMin = 20;
                final int white = 220;
                final int k1;
                if (dE <= dEMin) {
                k1 = white;
                } else if ( dE > dEMax) {
                k1 = 0;
                } else {
                final double m = - white / (dEMax - dEMin);
                final double kk = -m * dEMax;
                k1 = (int)(m * dE + kk);
                }
                r1 = k1 + (255 - k1) * r1 / 255;
                g1 = k1 + (255 - k1) * g1 / 255;
                b1 = k1 + (255 - k1) * b1 / 255;
                if (r1 > 255)
                r1 = 255;
                if (g1 > 255)
                g1 = 255;
                if (b1 > 255)
                b1 = 255;
                final int rgb = (int)(r1 << 16 | g1 << 8 | b1 | 0xff000000);
                 */


                difImg.setRGB(c, r, rgb);
            }
        }


        return difImg;

    }

    /**
     * Takes a screenshot, simulates color-impaired vision on the screenshot and 
     * shows the simulation.
     * @param panel A raster image that is displayed over the simulated image.
     */
    private void simulateAndShow(Image panel) {

        try {
            // wait for a few milliseconds until the menu has faded out.
            Thread.sleep(SLEEP_BEFORE_SCREENSHOT_MILLISECONDS);

            final boolean simulationVisible = Screen.getScreens().size() > 0;

            // detect all attached screens
            if (!simulationVisible) {
                Screen.detectScreens();
            }

            // simulate color-impaired vision for all attached screens
            for (Screen screen : Screen.getScreens()) {

                // don't take a screenshot when a color-impaired simulation 
                // is currently visible. Instead, use the same screenshot again.
                if (!simulationVisible) {
                    screen.takeScreenshot();
                }

                // apply a simulation filter to the screenshot
                BufferedImage img = this.simulator.filter(screen.screenshotImage);
                //img = computeDifference(img, screen.screenshotImage);

                // ImageIO.write(img, "png", new File("screen" + Screen.getScreens().indexOf(screen) + ".png"));

                // show the result of the simulation in a window
                screen.showSimulationImage(img, this, panel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                this.switchToNormalVision();
            } catch (Exception exc) {
            }
            ColorOracle.showErrorMessage(e.getMessage(), false);
            e.printStackTrace();
        }

    }

    private static void showErrorMessage(String msg, boolean showExitButton) {

        if (msg == null || msg.trim().length() < 3) {
            msg = "An error occurred.";
        } else {
            msg = msg.trim();
        }
        String title = "Color Oracle Error";
        Object[] options = new Object[]{"Exit Color Oracle"};
        javax.swing.JOptionPane.showOptionDialog(null, msg, title,
                JOptionPane.DEFAULT_OPTION,
                javax.swing.JOptionPane.ERROR_MESSAGE,
                null,
                showExitButton ? options : null,
                showExitButton ? options[0] : null);
    }

    /**
     * Updates the menu: makes sure only one item has a check mark, enables or
     * disables the menu items, and changes the title of the Save menu item.
     */
    private void updateMenuState() {

        // make sure only one menu item is checked
        this.normalMenuItem.setState(this.currentSimulation == Simulation.normal);
        this.deutanMenuItem.setState(this.currentSimulation == Simulation.deutan);
        this.protanMenuItem.setState(this.currentSimulation == Simulation.protan);
        this.tritanMenuItem.setState(this.currentSimulation == Simulation.tritan);

        // disable menu items if the Save dialog is in the foreground.
        this.normalMenuItem.setEnabled(!this.currentlySavingImage);
        this.deutanMenuItem.setEnabled(!this.currentlySavingImage);
        this.protanMenuItem.setEnabled(!this.currentlySavingImage);
        this.tritanMenuItem.setEnabled(!this.currentlySavingImage);
        this.aboutMenuItem.setEnabled(!this.currentlySavingImage);

        // Save item is disabled when we are not currently showing a simulation.
        // It is also disabled when the save-as dialog is currently open.
        this.saveMenuItem.setEnabled(this.currentSimulation != Simulation.normal && !this.currentlySavingImage);

        // change the title of the save item to the current simulation.
        String saveMenuLabel;
        if (this.currentSimulation != Simulation.normal) {
            saveMenuLabel = "Save " + this.currentSimulationName() + " Image...";
        } else {
            saveMenuLabel = "Save Filtered Screen Image...";
        }
        this.saveMenuItem.setLabel(saveMenuLabel);
    }

    /**
     * Event handler for the About menu item. Shows the about dialog.
     */
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {

        // hide the simulation window. Otherwise the about dialog would cover 
        // the simulation window and capture events, which is bad if the user
        // clicks the button that starts the default web browser. The simulation
        // windows would remain visible and be covered by the web browser.
        this.switchToNormalVision();

        if (this.aboutDialog == null) {
            this.aboutDialog = new javax.swing.JDialog();
            aboutDialog.setTitle("About Color Oracle");
            aboutDialog.getContentPane().add(new AboutPanel(), BorderLayout.CENTER);
            aboutDialog.setResizable(false);
            aboutDialog.pack();
            aboutDialog.setLocationRelativeTo(null);// center the frame
        }
        aboutDialog.setVisible(true);
    }

    /**
     * Event handler for the Save menu item. Saves the current simulation to
     * a PNG file.
     */
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        this.currentlySavingImage = true;
        try {

            // first get the simulation image
            int screenID = 0; // default is the first screen
            final int screenCount = Screen.getScreens().size();
            if (screenCount > 1) {
                // ask the user for the screen to save when there is more than one
                ArrayList<String> screenNames = new ArrayList<String>(screenCount);
                for (int i = 1; i <= screenCount; i++) {
                    screenNames.add("Screen " + i);
                }
                String screenName = (String) JOptionPane.showInputDialog(null,
                        "Select a Screen:",
                        "Save Filtered Screen Image",
                        JOptionPane.QUESTION_MESSAGE, null,
                        screenNames.toArray(), null);
                if (screenName == null) {
                    return; // user canceled
                }
                screenID = screenNames.indexOf(screenName);
            }
            Image simImg = Screen.getScreens().get(screenID).simulationWindow.getImage();

            // then hide and release the simulation windows
            this.hideSimulation();
            if (simImg == null) {
                throw new Exception("No image to save.");
            }

            // construct file name
            String fileName = this.currentSimulationName() + ".png";

            // disable menu items
            this.updateMenuState();

            // ask user for file to save the image
            String filePath = ColorOracle.askFile(null, "Save Image", fileName, false);
            if (filePath == null) {
                return;
            }

            // make sure the file has a png extension.
            filePath = forceFileNameExtension(filePath, "png");

            // write the image to a file
            ImageIO.write((BufferedImage) simImg, "png", new File(filePath));
        } catch (Exception e) {
            this.hideSimulation();
            e.printStackTrace();
            String msg = "An unexpected error occurred. \n" + e.getMessage();
            String title = "Color Oracle Error";
            javax.swing.JOptionPane.showMessageDialog(null, msg, title,
                    javax.swing.JOptionPane.ERROR_MESSAGE, null);
        } finally {
            this.currentlySavingImage = false;

            // re-enable menu items
            this.currentSimulation = Simulation.normal;
            this.updateMenuState();
        }
    }

    /**
     * Simulate color impaired vision and display it.
     */
    private void simulate(Simulation simulationType) {
        try {
            // remember the current simulation
            this.currentSimulation = simulationType;

            this.updateMenuState();

            // hide the about dialog before taking a screenshot
            if (this.aboutDialog != null) {
                this.aboutDialog.setVisible(false);
            }

            // take a screenshot, simulate and show the result
            this.simulator.simulate(simulationType);
            switch (simulationType) {
                case deutan:
                    this.simulateAndShow(this.deutanPanel);
                    break;
                case protan:
                    this.simulateAndShow(this.protanPanel);
                    break;
                case tritan:
                    this.simulateAndShow(this.tritanPanel);
                    break;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            this.switchToNormalVision();
        }
    }

    /**
     * Change to normal vision. Hides the window containing the simulated vision,
     * if it is currently visible.
     */
    public void switchToNormalVision() {
        // remember the current simulation
        this.currentSimulation = Simulation.normal;

        this.updateMenuState();

        // hide the window
        this.hideSimulation();
    }

    /**
     * Event handler that is called when the user types a key and the 
     * window displaying the simulated color blind image has the current key focus.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        this.switchToNormalVision();
    }

    /**
     * Event handler that is called when the user presses a key down and the 
     * window displaying the simulated color blind image has the current key focus.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        this.switchToNormalVision();
    }

    /**
     * Event handler that is called when the user releases a key and the 
     * window displaying the simulated color blind image has the current key focus.
     */
    @Override
    public void keyReleased(KeyEvent e) {
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4455060
        /*int code = e.getKeyCode();
        if (code == KeyEvent.VK_PRINTSCREEN) {
            JOptionPane.showMessageDialog(null, code);
        }*/
        this.switchToNormalVision();
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    /**
     * Event handler that is called when the window displaying the simulated 
     * color blind image is deactivated.
     */
    @Override
    public void windowDeactivated(WindowEvent e) {
        try {

            // e.getOppositeWindow() returns null when a window of another
            // application is activated. Don't hide the windows on a system
            // with multiple screens when the user switches between our windows.
            if (e.getOppositeWindow() != null) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime > this.timeOfLastFocusLost) {
                this.timeOfLastFocusLost = currentTime;
            }
            this.startDeactivatingTimer();
        } catch (Exception exc) {
            exc.printStackTrace();
            this.switchToNormalVision();
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    /**
     * Event handler that is called when the window displaying the simulated 
     * colorblind image looses the focus.
     */
    @Override
    public void focusLost(FocusEvent e) {
        try {
            // e.ggetOppositeComponent() returns null when a window of another
            // application is activated. Don't hide the windows on a system
            // with multiple screens when the user switches between our windows.
            if (e.getOppositeComponent() != null) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime > this.timeOfLastFocusLost) {
                this.timeOfLastFocusLost = currentTime;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            this.switchToNormalVision();
        }
    }

    private void startDeactivatingTimer() {
        int numberOfMillisecondsInTheFuture = 300;
        long execTime = System.currentTimeMillis() + numberOfMillisecondsInTheFuture;
        Date timeToRun = new Date(execTime);
        java.util.Timer timer = new java.util.Timer();

        timer.schedule(new java.util.TimerTask() {

            @Override
            public void run() {
                long dT = Math.abs(timeOfLastFocusLost - timeOfLastClickOnTrayIcon);
                // System.out.println("Timer time difference: " + dT);
                if (dT > 300) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            switchToNormalVision();
                        }
                    });
                }
            }
        }, timeToRun);
    }

    /**
     * Ask the user for a file to load or to write to. Uses the awt FileDialog 
     * on Mac and the JFileChooser on other platforms.
     * @param frame A Frame for which to display the dialog. Cannot be null.
     * @param message A message that will be displayed in the dialog.
     * @param defaultFile The default file name.
     * @param load Pass true if an existing file for reading should be selected. Pass false if a new
     * file for writing should be specified.
     * @return A path to the file, including the file name.
     */
    public static String askFile(java.awt.Frame frame, String message,
            String defaultFile, boolean load) {

        if (Sys.isMacOSX()) {
            final int flag = load ? FileDialog.LOAD : FileDialog.SAVE;

            // build dummy Frame if none is passed as parameter.
            if (frame == null) {
                frame = new Frame();
            }

            FileDialog fd = new FileDialog(frame, message, flag);
            fd.setFile(defaultFile);
            fd.setVisible(true);
            String fileName = fd.getFile();
            String directory = fd.getDirectory();
            if (fileName == null || directory == null) {
                return null;
            }
            return directory + fileName;
        } else {
            JFileChooser fc = new JFileChooser();

            // set default file
            try {
                File f = new File(new File(defaultFile).getCanonicalPath());
                fc.setSelectedFile(f);
            } catch (Exception e) {
            }

            int result = JFileChooser.ERROR_OPTION;
            if (load) {
                // Show open dialog
                result = fc.showOpenDialog(frame);
            } else {
                // Show save dialog
                result = fc.showSaveDialog(frame);
            }

            if (result != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            File selFile = fc.getSelectedFile();
            if (selFile == null) {
                return null;
            }

            return selFile.getPath();
        }
    }

    /**
     * Makes sure that a given name of a file has a certain file extension.<br>
     * Existing file extension that are different from the required one, are not
     * removed, nor is the file name altered in any other way.
     * @param fileName The name of the file.
     * @param ext The extension of the file that will be appended if necessary.
     * @return The new file name with the required extension.
     */
    public static String forceFileNameExtension(String fileName, String ext) {

        String fileNameLower = fileName.toLowerCase();
        String extLower = ext.toLowerCase();

        // test if the fileName has the required extension
        if (!fileNameLower.endsWith("." + extLower)) {

            // fileName has wrong extension: add an extension

            if (!fileNameLower.endsWith(".")) // add separating dot if required
            {
                fileName = fileName.concat(".");
            }
            fileName = fileName.concat(ext);   // add extension

            // the fileName was just changed: test if there exists a file with the same name
            if (new File(fileName).exists()) {
                String msg = "The file \"" + fileName + "\" already exists.\n"
                        + "Please try again and add the extension \"." + ext + "\" to the file name.";
                String title = "File Already Exists";
                javax.swing.JOptionPane.showMessageDialog(null, msg, title,
                        javax.swing.JOptionPane.ERROR_MESSAGE, null);
                return null;
            }
        }

        return fileName;

    }

    /**
     * Hide the simulation when the mouse wheel is scrolled.
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() != 0) {
            this.switchToNormalVision();
        }
    }
}
