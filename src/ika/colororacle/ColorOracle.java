/*
 * ColorOraclerOracle.java
 *
 * Created on February 4, 2007, 10:20 PM
 *
 */
package ika.colororacle;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * ColorOracle is the main class of the program. It creates the tray icon and
 * handles all events, except for mouse events, which are handled by
 * ImageDisplayWithPanel.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ColorOracle extends WindowAdapter implements KeyListener, FocusListener, MouseWheelListener {

    /**
     * A tooltip that is displayed when the mouse hovers over the tray icon.
     */
    private static final String TOOLTIP = "Simulate Color-impaired Vision with Color Oracle";

    /**
     * An error message that is displayed if the system does not support tray
     * icons.
     */
    private static final String TRAYICONS_NOT_SUPPORTED_MESSAGE
            = "Tray icons are not supported on this system. "
            + "Color Oracle will therefore quit.";

    /**
     * The name of the icon that is placed in the task bar.
     */
    private static final String MENUICON = "menuIcon.gif";

    /**
     * The information panel image for deuteranopia.
     */
    private final Image deutanPanel = loadImage("deutanpanel.png");

    /**
     * The information panel image for protanopia.
     */
    private final Image protanPanel = loadImage("protanpanel.png");

    /**
     * The information panel image for tritanopia.
     */
    private final Image tritanPanel = loadImage("tritanpanel.png");

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
     * Keep track of the current type of color-impairment simulation.
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
    private final Simulator simulator = new Simulator();

    /**
     * A menu item for normal vision that will be added to the tray menu.
     */
    private final CheckboxMenuItem normalMenuItem = new CheckboxMenuItem();

    /**
     * A menu item for deuteranopia that will be added to the tray menu.
     */
    private final CheckboxMenuItem deutanMenuItem = new CheckboxMenuItem();

    /**
     * A menu item for protanopia that will be added to the tray menu.
     */
    private final CheckboxMenuItem protanMenuItem = new CheckboxMenuItem();

    /**
     * A menu item for tritanopia that will be added to the tray menu.
     */
    private final CheckboxMenuItem tritanMenuItem = new CheckboxMenuItem();

    /**
     * The About menu item that will be added to the tray menu.
     */
    private final MenuItem aboutMenuItem = new MenuItem();

    private long timeOfLastClickOnTrayIcon = 0;

    private long timeOfLastFocusLost = 0;

    /**
     * Entry point for the Color Oracle application.
     *
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
        String nativeLF = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(nativeLF);
        } catch (Exception ex) {
            Logger.getLogger(ColorOracle.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (SecurityException ex) {
            Logger.getLogger(ColorOracle.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (Exception ex) {
            ColorOracle.showErrorMessage("Access to the system tray or "
                    + "notification area \nis not supported on your system.",
                    true);
            Logger.getLogger(ColorOracle.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
            return;
        }

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    new ColorOracle();
                } catch (Exception ex) {
                    Logger.getLogger(ColorOracle.class.getName()).log(Level.SEVERE, null, ex);
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
    private static void setOptionPaneIcons(String iconPath) {
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
        initTrayIcon();
    }

    /**
     * Loads a raster icon from the /ika/icons/ folder.
     *
     * @param name The name of the icon.
     * @description A description of the icon that is attached to it.
     * @return An ImageIcon.
     *
     */
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
     *
     * @param name The name of the icon.
     * @return The icon as Image object.
     *
     */
    public static Image loadImage(String name) {
        ImageIcon icon = loadImageIcon(name, "");
        return icon.getImage();
    }

    /**
     * Hides the color-impairment simulation.
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
        PopupMenu menu = initMenu();

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
     *
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
        switch (currentSimulation) {
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

    /**
     * Takes a screenshot, simulates color-impaired vision on the screenshot and
     * shows the simulation.
     *
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
                BufferedImage img = simulator.filter(screen.screenshotImage);
                //img = computeDifference(img, screen.screenshotImage);

                // ImageIO.write(img, "png", new File("screen" + Screen.getScreens().indexOf(screen) + ".png"));
                // show the result of the simulation in a window
                screen.showSimulationImage(img, this, panel);
            }
        } catch (Exception ex) {
            try {
                switchToNormalVision();
            } catch (Exception exc) {
            }
            ColorOracle.showErrorMessage(ex.getMessage(), false);
            Logger.getLogger(ColorOracle.class.getName()).log(Level.SEVERE, null, ex);
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
        normalMenuItem.setState(currentSimulation == Simulation.normal);
        deutanMenuItem.setState(currentSimulation == Simulation.deutan);
        protanMenuItem.setState(currentSimulation == Simulation.protan);
        tritanMenuItem.setState(currentSimulation == Simulation.tritan);

        // disable menu items if the Save dialog is in the foreground.
        normalMenuItem.setEnabled(!currentlySavingImage);
        deutanMenuItem.setEnabled(!currentlySavingImage);
        protanMenuItem.setEnabled(!currentlySavingImage);
        tritanMenuItem.setEnabled(!currentlySavingImage);
        aboutMenuItem.setEnabled(!currentlySavingImage);
    }

    /**
     * Event handler for the About menu item. Shows the about dialog.
     */
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {

        // hide the simulation window. Otherwise the about dialog would cover 
        // the simulation window and capture events, which is bad if the user
        // clicks the button that starts the default web browser. The simulation
        // windows would remain visible and be covered by the web browser.
        switchToNormalVision();

        if (aboutDialog == null) {
            aboutDialog = new javax.swing.JDialog();
            aboutDialog.setTitle("About Color Oracle");
            aboutDialog.getContentPane().add(new AboutPanel(), BorderLayout.CENTER);
            aboutDialog.setResizable(false);
            aboutDialog.pack();
            aboutDialog.setLocationRelativeTo(null);// center the frame
        }
        aboutDialog.setVisible(true);
    }

    /**
     * Simulate color impaired vision and display it.
     */
    private void simulate(Simulation simulationType) {
        try {
            // remember the current simulation
            currentSimulation = simulationType;

            updateMenuState();

            // hide the about dialog before taking a screenshot
            if (aboutDialog != null) {
                aboutDialog.setVisible(false);
            }

            // take a screenshot, simulate and show the result
            simulator.simulate(simulationType);
            switch (simulationType) {
                case deutan:
                    simulateAndShow(deutanPanel);
                    break;
                case protan:
                    simulateAndShow(protanPanel);
                    break;
                case tritan:
                    simulateAndShow(tritanPanel);
                    break;
            }
        } catch (Exception ex) {
            Logger.getLogger(ColorOracle.class.getName()).log(Level.SEVERE, null, ex);
            switchToNormalVision();
        }
    }

    /**
     * Change to normal vision. Hides the window containing the simulated
     * vision, if it is currently visible.
     */
    public void switchToNormalVision() {
        // remember the current simulation
        currentSimulation = Simulation.normal;

        updateMenuState();

        // hide the window
        hideSimulation();
    }

    /**
     * Event handler that is called when the user types a key and the window
     * displaying the simulated color blind image has the current key focus.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        switchToNormalVision();
    }

    /**
     * Event handler that is called when the user presses a key down and the
     * window displaying the simulated color blind image has the current key
     * focus.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        switchToNormalVision();
    }

    /**
     * Event handler that is called when the user releases a key and the window
     * displaying the simulated color blind image has the current key focus.
     */
    @Override
    public void keyReleased(KeyEvent e) {
        switchToNormalVision();
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
            if (currentTime > timeOfLastFocusLost) {
                timeOfLastFocusLost = currentTime;
            }
            startDeactivatingTimer();
        } catch (Exception ex) {
            Logger.getLogger(ColorOracle.class.getName()).log(Level.SEVERE, null, ex);
            switchToNormalVision();
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
            if (currentTime > timeOfLastFocusLost) {
                timeOfLastFocusLost = currentTime;
            }
        } catch (Exception ex) {
            Logger.getLogger(ColorOracle.class.getName()).log(Level.SEVERE, null, ex);
            switchToNormalVision();
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
     * Makes sure that a given name of a file has a certain file extension.<br>
     * Existing file extension that are different from the required one, are not
     * removed, nor is the file name altered in any other way.
     *
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
     *
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() != 0) {
            switchToNormalVision();
        }
    }
}
