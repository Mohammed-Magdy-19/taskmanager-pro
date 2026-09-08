package com.taskmanager.config.wizard;

import com.taskmanager.view.theme.AppTheme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Custom-painted step progress indicator for multi-step wizards.
 * Renders connected circular milestone markers illustrating completed, active,
 * and upcoming workflow stages.
 */
public class WizardStepIndicator extends JPanel {

    private static final int DOT_DIAMETER = 18;
    private static final int PREFERRED_HEIGHT = 44;

    private final int totalSteps;
    private int currentStep;

    /**
     * Constructs a step indicator with a set number of stages.
     *
     * @param totalSteps total count of wizard steps
     */
    public WizardStepIndicator(int totalSteps) {
        this.totalSteps = Math.max(1, totalSteps);
        this.currentStep = 0;
        setOpaque(false);
        setPreferredSize(new Dimension(300, PREFERRED_HEIGHT));
    }

    /**
     * Updates the active step index and requests a visual redraw.
     *
     * @param currentStep zero-based index of the active step
     */
    public void setCurrentStep(int currentStep) {
        this.currentStep = Math.max(0, Math.min(currentStep, totalSteps - 1));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        int marginX = AppTheme.SPACING * 4;
        int usableWidth = width - (marginX * 2);
        int spacing = totalSteps > 1 ? usableWidth / (totalSteps - 1) : 0;

        // Draw connecting track lines
        g2.setStroke(new BasicStroke(2.0f));
        for (int i = 0; i < totalSteps - 1; i++) {
            int x1 = marginX + (i * spacing);
            int x2 = marginX + ((i + 1) * spacing);
            g2.setColor(i < currentStep ? AppTheme.PRIMARY : AppTheme.STEP_INACTIVE);
            g2.drawLine(x1, centerY, x2, centerY);
        }

        // Draw milestone dots
        int radius = DOT_DIAMETER / 2;
        for (int i = 0; i < totalSteps; i++) {
            int dotX = marginX + (i * spacing) - radius;
            int dotY = centerY - radius;

            if (i < currentStep) {
                // Completed step: solid fill with small white checkmark
                g2.setColor(AppTheme.PRIMARY);
                g2.fillOval(dotX, dotY, DOT_DIAMETER, DOT_DIAMETER);
                drawCheckmark(g2, dotX + radius, dotY + radius);
            } else if (i == currentStep) {
                // Active step: primary halo with solid white center
                g2.setColor(AppTheme.PRIMARY);
                g2.fillOval(dotX, dotY, DOT_DIAMETER, DOT_DIAMETER);
                g2.setColor(AppTheme.TEXT_INVERTED);
                int innerD = 6;
                g2.fillOval(dotX + radius - (innerD / 2), dotY + radius - (innerD / 2), innerD, innerD);
            } else {
                // Upcoming step: outlined neutral dot
                g2.setColor(AppTheme.BG_CARD);
                g2.fillOval(dotX, dotY, DOT_DIAMETER, DOT_DIAMETER);
                g2.setColor(AppTheme.STEP_INACTIVE);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(dotX, dotY, DOT_DIAMETER, DOT_DIAMETER);
            }
        }

        g2.dispose();
    }

    private void drawCheckmark(Graphics2D g2, int cx, int cy) {
        g2.setColor(AppTheme.TEXT_INVERTED);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx - 4, cy, cx - 1, cy + 3);
        g2.drawLine(cx - 1, cy + 3, cx + 4, cy - 3);
    }
}
