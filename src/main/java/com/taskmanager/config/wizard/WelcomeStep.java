package com.taskmanager.config.wizard;

import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Initial introductory panel of the configuration wizard.
 * Presents a branded heading with a custom-painted geometric icon and setup
 * guidance.
 */
public class WelcomeStep extends CardPanel implements WizardStepPanel {

    private static final String TITLE_TEXT = "Welcome to Task Manager";
    private static final String BODY_TEXT = "This setup wizard will help you configure essential settings before launching Task Manager:\n\n"
            + "  • Database storage location\n"
            + "  • Visual theme and language preferences\n"
            + "  • Task notification and reminder intervals\n\n"
            + "Click 'Next' to begin configuration.";

    /**
     * Constructs the WelcomeStep panel with a custom app icon and typography.
     */
    public WelcomeStep() {
        super(new BorderLayout(AppTheme.SPACING, AppTheme.SPACING));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACING, 0));
        headerPanel.setOpaque(false);
        headerPanel.add(new BrandLogoIcon());

        JLabel titleLabel = new JLabel(TITLE_TEXT);
        titleLabel.setFont(AppTheme.FONT_HEADING);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel);

        add(headerPanel, BorderLayout.NORTH);

        JTextArea bodyArea = new JTextArea(BODY_TEXT);
        bodyArea.setFont(AppTheme.FONT_BODY);
        bodyArea.setForeground(AppTheme.TEXT_BODY);
        bodyArea.setEditable(false);
        bodyArea.setFocusable(false);
        bodyArea.setOpaque(false);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACING, AppTheme.SPACING / 2, 0, 0));

        add(bodyArea, BorderLayout.CENTER);
    }

    @Override
    public void commit(WizardData data) {
        // Welcome step contains no user-editable inputs
    }

    /**
     * Lightweight custom-painted geometric app mark icon in AppTheme.PRIMARY.
     */
    private static class BrandLogoIcon extends JComponent {

        private static final int ICON_SIZE = 34;

        BrandLogoIcon() {
            setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Rounded gradient/accent backdrop
            g2.setColor(AppTheme.PRIMARY);
            g2.fillRoundRect(0, 0, ICON_SIZE, ICON_SIZE, AppTheme.RADIUS_SMALL, AppTheme.RADIUS_SMALL);

            // Stylized checkmark mark
            g2.setColor(AppTheme.TEXT_INVERTED);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(9, 17, 15, 23);
            g2.drawLine(15, 23, 25, 11);

            g2.dispose();
        }
    }
}
