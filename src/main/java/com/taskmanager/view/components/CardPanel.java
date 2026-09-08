package com.taskmanager.view.components;

import com.taskmanager.view.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Reusable container panel rendering a modern rounded-card aesthetic with anti-aliasing
 * and a warm tinted ambient shadow.
 */
public class CardPanel extends JPanel {

    private final Color backgroundColor;
    private final Color borderColor;
    private final int cornerRadius;
    private final boolean drawShadow;

    /**
     * Constructs a CardPanel with default card colors, corner radius, and layout.
     *
     * @param layout layout manager for child components
     */
    public CardPanel(LayoutManager layout) {
        this(layout, AppTheme.BG_CARD, AppTheme.BORDER_CARD, AppTheme.RADIUS, true);
    }

    /**
     * Constructs a CardPanel with a default layout and card styling.
     */
    public CardPanel() {
        this(null, AppTheme.BG_CARD, AppTheme.BORDER_CARD, AppTheme.RADIUS, true);
    }

    /**
     * Fully parameterized constructor for customizable card styling.
     *
     * @param layout layout manager for child components
     * @param backgroundColor fill color of the card
     * @param borderColor outline stroke color
     * @param cornerRadius arc width and height
     * @param drawShadow whether to render a subtle ambient shadow
     */
    public CardPanel(LayoutManager layout, Color backgroundColor, Color borderColor, int cornerRadius, boolean drawShadow) {
        if (layout != null) {
            setLayout(layout);
        }
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.cornerRadius = cornerRadius;
        this.drawShadow = drawShadow;

        setOpaque(false);
        int padding = AppTheme.SPACING + (drawShadow ? 4 : 0);
        setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int shadowOffset = drawShadow ? 2 : 0;
        int width = getWidth() - 1 - shadowOffset;
        int height = getHeight() - 1 - shadowOffset;

        if (drawShadow) {
            g2.setColor(AppTheme.SHADOW_COLOR);
            g2.fillRoundRect(shadowOffset, shadowOffset, width, height, cornerRadius, cornerRadius);
        }

        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        g2.setColor(borderColor);
        g2.drawRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);
    }
}
