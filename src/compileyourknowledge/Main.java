package compileyourknowledge;

import compileyourknowledge.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        // Metal look and feel keeps the custom buttons identical on macOS and Windows.
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}