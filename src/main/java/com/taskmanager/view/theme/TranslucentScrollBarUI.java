package com.taskmanager.view.theme;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern floating overlay scrollbar UI.
 * Paints no track background and renders a rounded translucent thumb that darkens on hover.
 */
public class TranslucentScrollBarUI extends BasicScrollBarUI {

    private static final int THUMB_RADIUS = 8;
    private static final int THUMB_MARGIN = 2;
    private static final int SCROLLBAR_THICKNESS = 9;

    private static final Color THUMB_IDLE = AppTheme.SCROLLBAR_THUMB_IDLE;
    private static final Color THUMB_HOVER = AppTheme.SCROLLBAR_THUMB_HOVER;

    private boolean hovering = false;
    private MouseAdapter hoverListener;

    @Override
    protected void installListeners() {
        super.installListeners();
        hoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                if (scrollbar != null) {
                    scrollbar.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                if (scrollbar != null) {
                    scrollbar.repaint();
                }
            }
        };
        if (scrollbar != null) {
            scrollbar.addMouseListener(hoverListener);
            scrollbar.addMouseMotionListener(hoverListener);
        }
    }

    @Override
    protected void uninstallListeners() {
        if (scrollbar != null && hoverListener != null) {
            scrollbar.removeMouseListener(hoverListener);
            scrollbar.removeMouseMotionListener(hoverListener);
        }
        super.uninstallListeners();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // Intentionally empty to render as a floating overlay without a solid track
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(hovering ? THUMB_HOVER : THUMB_IDLE);
        int x = thumbBounds.x + THUMB_MARGIN;
        int y = thumbBounds.y;
        int width = Math.max(1, thumbBounds.width - (THUMB_MARGIN * 2));
        int height = Math.max(1, thumbBounds.height);

        g2.fillRoundRect(x, y, width, height, THUMB_RADIUS, THUMB_RADIUS);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(SCROLLBAR_THICKNESS, SCROLLBAR_THICKNESS);
    }
}
