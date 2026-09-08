package com.taskmanager.service;

import com.taskmanager.concurrency.ThreadPoolManager;
import com.taskmanager.event.EventBus;
import com.taskmanager.event.TaskReminderEvent;
import com.taskmanager.model.Task;
import com.taskmanager.util.AppLogger;
import com.taskmanager.util.SwingSafe;
import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Service implementation for delivering task reminders to the user.
 * Subscribes to {@link TaskReminderEvent} via {@link EventBus}.
 * Displays desktop notifications using {@link SystemTray} when supported,
 * falling back to a non-modal Swing notification toast dialog.
 */
public class NotificationServiceImpl implements NotificationService {

    private static final AppLogger LOGGER = AppLogger.getLogger(NotificationServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TrayIcon trayIcon;

    /**
     * Constructs the NotificationServiceImpl, initializes tray integration if available,
     * and registers an event listener for task reminders.
     */
    public NotificationServiceImpl() {
        initTray();
        EventBus.getInstance().subscribe(TaskReminderEvent.class, event -> showReminder(event.getTask()));
    }

    private void initTray() {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            LOGGER.info("SystemTray is not supported or environment is headless; using dialog fallback.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image iconImage = createDefaultTrayImage();
            trayIcon = new TrayIcon(iconImage, "Task Manager");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            LOGGER.info("SystemTray initialized successfully.");
        } catch (AWTException | SecurityException e) {
            LOGGER.warn("Failed to initialize SystemTray icon: " + e.getMessage() + ". Using fallback.");
            trayIcon = null;
        }
    }

    @Override
    public void showReminder(Task task) {
        if (task == null) {
            return;
        }

        String dueStr = (task.getDueDate() != null) ? " (Due: " + task.getDueDate().format(DATE_FORMATTER) + ")" : "";
        LOGGER.info("Notification fired for task: " + task.getTitle() + dueStr);

        if (trayIcon != null) {
            trayIcon.displayMessage(
                    "Task Reminder",
                    task.getTitle() + dueStr,
                    TrayIcon.MessageType.INFO
            );
        } else if (!GraphicsEnvironment.isHeadless()) {
            SwingSafe.run(() -> showFallbackDialog(task, dueStr));
        }
    }

    private void showFallbackDialog(Task task, String dueStr) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Task Reminder");
        dialog.setModal(false);
        dialog.setAlwaysOnTop(true);
        dialog.setUndecorated(true);

        CardPanel card = new CardPanel(new BorderLayout(0, AppTheme.SPACING / 2));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.PRIMARY, 1, true),
                BorderFactory.createEmptyBorder(AppTheme.SPACING, AppTheme.SPACING, AppTheme.SPACING, AppTheme.SPACING)
        ));

        JPanel contentBox = new JPanel();
        contentBox.setLayout(new BoxLayout(contentBox, BoxLayout.Y_AXIS));
        contentBox.setOpaque(false);

        JLabel titleLabel = new JLabel("⏰ Task Reminder: " + task.getTitle());
        titleLabel.setFont(AppTheme.FONT_SUBHEADING);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel descLabel = new JLabel((task.getDescription() != null && !task.getDescription().isBlank())
                ? task.getDescription() : "This task is due soon." + dueStr);
        descLabel.setFont(AppTheme.FONT_BODY);
        descLabel.setForeground(AppTheme.TEXT_BODY);

        contentBox.add(titleLabel);
        contentBox.add(Box.createVerticalStrut(6));
        contentBox.add(descLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        JButton dismissButton = new JButton("Dismiss");
        dismissButton.setFont(AppTheme.FONT_BUTTON);
        dismissButton.setBackground(AppTheme.PRIMARY);
        dismissButton.setForeground(AppTheme.TEXT_INVERTED);
        dismissButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(dismissButton);

        card.add(contentBox, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.pack();
        dialog.setSize(new Dimension(340, 140));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        // Auto-dismiss after 8 seconds using ThreadPoolManager schedule (Rule 8 compliant)
        ThreadPoolManager.getInstance().schedule(() -> SwingSafe.run(dialog::dispose), 8, TimeUnit.SECONDS);
    }

    private Image createDefaultTrayImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(AppTheme.PRIMARY);
        g2.fillOval(1, 1, 14, 14);
        g2.setColor(AppTheme.TEXT_INVERTED);
        g2.fillRect(7, 3, 2, 7);
        g2.fillRect(7, 11, 2, 2);
        g2.dispose();
        return image;
    }

    /**
     * Cleans up system tray icons if registered.
     */
    public void cleanup() {
        if (trayIcon != null && SystemTray.isSupported()) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
                trayIcon = null;
            } catch (Exception ignored) {
                // Best-effort cleanup
            }
        }
    }
}
