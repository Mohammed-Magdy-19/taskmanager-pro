package com.taskmanager.view;

import com.taskmanager.controller.DashboardController;
import com.taskmanager.event.BackupCompletedEvent;
import com.taskmanager.event.EventBus;
import com.taskmanager.event.TaskProcessedEvent;
import com.taskmanager.service.DashboardStats;
import com.taskmanager.util.AppLogger;
import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Visual dashboard panel displaying high-level task metrics and overall completion progress.
 * Subscribes to {@link EventBus} events to automatically refresh whenever tasks change
 * or backups/restores occur, ensuring zero EDT polling loops.
 */
public class DashboardPanel extends JPanel {

    private static final AppLogger LOGGER = AppLogger.getLogger(DashboardPanel.class);

    private final DashboardController dashboardController;

    private final JLabel totalCountLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel pendingCountLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel inProgressCountLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel completedCountLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel overdueCountLabel = new JLabel("0", SwingConstants.CENTER);

    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel progressSummaryLabel = new JLabel("0.0% completed", SwingConstants.CENTER);

    private final Consumer<TaskProcessedEvent> taskProcessedListener;
    private final Consumer<BackupCompletedEvent> backupCompletedListener;

    /**
     * Constructs the DashboardPanel connected to the given controller.
     *
     * @param dashboardController the controller computing dashboard statistics
     */
    public DashboardPanel(DashboardController dashboardController) {
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController must not be null");

        this.taskProcessedListener = event -> refreshStats();
        this.backupCompletedListener = event -> refreshStats();

        initLayout();
        initEventSubscription();
        refreshStats();
    }

    private void initLayout() {
        setLayout(new BorderLayout(AppTheme.SPACING, AppTheme.SPACING));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACING * 2, AppTheme.SPACING * 2,
                AppTheme.SPACING * 2, AppTheme.SPACING * 2));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel titleLabel = new JLabel("Dashboard Overview");
        titleLabel.setFont(AppTheme.FONT_HEADING);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("Real-time summary of task metrics and productivity progress");
        subtitleLabel.setFont(AppTheme.FONT_BODY);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);

        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitleLabel);

        JButton refreshBtn = new JButton("↻  Refresh");
        refreshBtn.setFont(AppTheme.FONT_BUTTON);
        refreshBtn.setBackground(AppTheme.BG_CARD);
        refreshBtn.setForeground(AppTheme.TEXT_BODY);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> refreshStats());

        headerPanel.add(titleBlock, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Center Area: Stat Cards + Progress Bar
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Stat Cards 5-column grid
        JPanel cardsGrid = new JPanel(new GridLayout(1, 5, AppTheme.SPACING, AppTheme.SPACING));
        cardsGrid.setOpaque(false);
        cardsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        cardsGrid.setPreferredSize(new Dimension(800, 120));

        cardsGrid.add(createMetricCard("TOTAL TASKS", totalCountLabel, AppTheme.PRIMARY));
        cardsGrid.add(createMetricCard("PENDING", pendingCountLabel, AppTheme.WARNING));
        cardsGrid.add(createMetricCard("IN PROGRESS", inProgressCountLabel, AppTheme.PILL_PROGRESS_FG));
        cardsGrid.add(createMetricCard("COMPLETED", completedCountLabel, AppTheme.PILL_COMPLETED_FG));
        cardsGrid.add(createMetricCard("OVERDUE", overdueCountLabel, AppTheme.DANGER));

        centerPanel.add(Box.createVerticalStrut(AppTheme.SPACING));
        centerPanel.add(cardsGrid);
        centerPanel.add(Box.createVerticalStrut(AppTheme.SPACING * 2));

        // Progress Section Card
        CardPanel progressCard = new CardPanel(new BorderLayout(AppTheme.SPACING, AppTheme.SPACING));
        progressCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JPanel progressHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        progressHeader.setOpaque(false);
        JLabel progressTitle = new JLabel("Overall Completion Progress");
        progressTitle.setFont(AppTheme.FONT_SUBHEADING);
        progressTitle.setForeground(AppTheme.TEXT_PRIMARY);
        progressHeader.add(progressTitle);

        progressBar.setStringPainted(true);
        progressBar.setFont(AppTheme.FONT_BODY_BOLD);
        progressBar.setForeground(AppTheme.SUCCESS);
        progressBar.setBackground(AppTheme.STEP_INACTIVE);
        progressBar.setPreferredSize(new Dimension(0, 24));

        progressSummaryLabel.setFont(AppTheme.FONT_BODY_BOLD);
        progressSummaryLabel.setForeground(AppTheme.TEXT_BODY);

        JPanel progressContent = new JPanel();
        progressContent.setLayout(new BoxLayout(progressContent, BoxLayout.Y_AXIS));
        progressContent.setOpaque(false);
        progressContent.add(Box.createVerticalStrut(8));
        progressContent.add(progressBar);
        progressContent.add(Box.createVerticalStrut(8));
        progressContent.add(progressSummaryLabel);

        progressCard.add(progressHeader, BorderLayout.NORTH);
        progressCard.add(progressContent, BorderLayout.CENTER);

        centerPanel.add(progressCard);
        centerPanel.add(Box.createVerticalGlue());

        add(centerPanel, BorderLayout.CENTER);
    }

    private CardPanel createMetricCard(String title, JLabel valueLabel, Color accentColor) {
        CardPanel card = new CardPanel(new BorderLayout(4, 4));
        card.setPreferredSize(new Dimension(140, 110));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(AppTheme.FONT_CAPTION);
        titleLabel.setForeground(AppTheme.TEXT_MUTED);

        Font numberFont = AppTheme.FONT_HEADING.deriveFont(Font.BOLD, 28f);
        valueLabel.setFont(numberFont);
        valueLabel.setForeground(accentColor);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void initEventSubscription() {
        EventBus.getInstance().subscribe(TaskProcessedEvent.class, taskProcessedListener);
        EventBus.getInstance().subscribe(BackupCompletedEvent.class, backupCompletedListener);
    }

    /**
     * Unsubscribes EventBus listeners when this view is disposed.
     */
    public void cleanup() {
        EventBus.getInstance().unsubscribe(TaskProcessedEvent.class, taskProcessedListener);
        EventBus.getInstance().unsubscribe(BackupCompletedEvent.class, backupCompletedListener);
    }

    /**
     * Refreshes the dashboard stats asynchronously via the controller.
     */
    public void refreshStats() {
        dashboardController.loadStats(this::updateStatsView, ex -> {
            LOGGER.error("Failed to load dashboard metrics", ex);
        });
    }

    /**
     * Updates UI components directly with computed stats on the EDT.
     *
     * @param stats the computed statistics
     */
    public void updateStatsView(DashboardStats stats) {
        if (stats == null) {
            stats = DashboardStats.empty();
        }
        totalCountLabel.setText(String.valueOf(stats.getTotalCount()));
        pendingCountLabel.setText(String.valueOf(stats.getPendingCount()));
        inProgressCountLabel.setText(String.valueOf(stats.getInProgressCount()));
        completedCountLabel.setText(String.valueOf(stats.getCompletedCount()));
        overdueCountLabel.setText(String.valueOf(stats.getOverdueCount()));

        int roundedPercentage = (int) Math.round(stats.getCompletionPercentage());
        progressBar.setValue(roundedPercentage);
        progressBar.setString(String.format("%.1f%%", stats.getCompletionPercentage()));
        progressSummaryLabel.setText(String.format("%.1f%% of tasks completed (%d of %d)",
                stats.getCompletionPercentage(), stats.getCompletedCount(), stats.getTotalCount()));
    }

    public JLabel getTotalCountLabel() {
        return totalCountLabel;
    }

    public JLabel getPendingCountLabel() {
        return pendingCountLabel;
    }

    public JLabel getInProgressCountLabel() {
        return inProgressCountLabel;
    }

    public JLabel getCompletedCountLabel() {
        return completedCountLabel;
    }

    public JLabel getOverdueCountLabel() {
        return overdueCountLabel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getProgressSummaryLabel() {
        return progressSummaryLabel;
    }
}
