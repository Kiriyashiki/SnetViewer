package xyz.hainaut.snetviewer;

import xyz.hainaut.snetviewer.ui.MainFrame;

import javax.swing.*;

/**
 * Entry point for the SnetViewer application.
 */
public class App {
    public static void main(String[] args) {
        // High-DPI and antialiasing system properties
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "SnetViewer");

        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel for native OS appearance
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fallback to default Swing Look & Feel
            }

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
