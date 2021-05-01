/*
 * AboutPanel.java
 *
 * Created on February 5, 2007, 10:00 AM
 */
package ika.colororacle;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;

/**
 * About panel for Color Oracle.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class AboutPanel extends javax.swing.JPanel {

    /**
     * Creates new form AboutPanel
     */
    public AboutPanel() {
        initComponents();
        versionLabel.setText("Java version " + System.getProperty("java.version"));
        // Before the Desktop API is used (since VM 1.6), first check
        // whether the API is supported by this particular
        // virtual machine (VM) on this particular host.
        homepageButton.setEnabled(Desktop.isDesktopSupported());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        javax.swing.JLabel textLabel = new javax.swing.JLabel();
        homepageButton = new javax.swing.JButton();
        javax.swing.JLabel iconLabel = new javax.swing.JLabel();
        javax.swing.JLabel titleLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        textLabel.setFont(textLabel.getFont().deriveFont(textLabel.getFont().getSize()-2f));
        textLabel.setText("<html><center>Version 1.3<br><br>Programming by<br>Bernhard Jenny, Monash University<br>and other contributors.<br><br>Ideas, Testing and Icon by<br>Nathaniel Vaughn Kelso<br><br>&copy; B. Jenny & N.V. Kelso 2006–2018.<br>CC-BY using the MIT License<br><br>For updates and further information see:<br></center></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(16, 21, 4, 21);
        add(textLabel, gridBagConstraints);

        homepageButton.setText("http://colororacle.org");
        homepageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homepageButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        add(homepageButton, gridBagConstraints);

        iconLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon_big.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
        add(iconLabel, gridBagConstraints);

        titleLabel.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        titleLabel.setText("<html>Color Oracle</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        add(titleLabel, gridBagConstraints);

        versionLabel.setFont(versionLabel.getFont().deriveFont(versionLabel.getFont().getSize()-2f));
        versionLabel.setText("Java version");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        add(versionLabel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Event handler for clicks on the home page button.
     */
    private void homepageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homepageButtonActionPerformed
        try {
            JButton button = (JButton) (evt.getSource());
            URI uri = new URI(button.getText());
            Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            Logger.getLogger(AboutPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_homepageButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton homepageButton;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables

}
