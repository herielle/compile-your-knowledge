package compileyourknowledge.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;

/**
 * A reusable retro "OS window" with a title bar, a text body, and a Back button.
 * How to Play, Credits, and the Start placeholder all use this one class.
 */
public class InfoPanel extends JPanel {

    public InfoPanel(MainFrame frame, String windowTitle, String bodyText) {
        setLayout(new GridBagLayout()); // centers the window

        JPanel window = new JPanel(new BorderLayout());
        window.setPreferredSize(new Dimension(640, 440));
        window.setBackground(Color.WHITE);
        window.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        JLabel titleBar = new JLabel("  " + windowTitle);
        titleBar.setOpaque(true);
        titleBar.setBackground(Theme.TITLE_BAR);
        titleBar.setForeground(Color.WHITE);
        titleBar.setFont(Theme.RETRO_FONT);
        titleBar.setPreferredSize(new Dimension(0, 30));

        JTextArea body = new JTextArea(bodyText);
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        body.setMargin(new java.awt.Insets(14, 16, 14, 16));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        footer.setBackground(new Color(225, 238, 250));
        GlassButton backButton = new GlassButton("Back to Menu");
        backButton.addActionListener(e -> frame.showScreen(MainFrame.SPLASH));
        footer.add(backButton);

        window.add(titleBar, BorderLayout.NORTH);
        window.add(scroll, BorderLayout.CENTER);
        window.add(footer, BorderLayout.SOUTH);
        add(window);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.paintSky(g2, getWidth(), getHeight());
        g2.dispose();
    }
}