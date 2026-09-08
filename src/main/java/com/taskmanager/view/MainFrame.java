package com.taskmanager.view;

import com.taskmanager.config.ConfigurationDialog;
import com.taskmanager.controller.TaskController;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.service.TaskServiceImpl;
import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * Main application window providing full desktop navigation, sidebar, and task
 * management.
 * Opens maximized with responsive reflow layout and modern warm aesthetics.
 */
public class MainFrame extends JFrame {

    private static final String VIEW_TASKS = "TASKS";
    private static final String VIEW_DASHBOARD = "DASHBOARD";
    private static final Dimension MIN_WINDOW_SIZE = new Dimension(1000, 650);

    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentArea = new JPanel(contentLayout);

    private TaskPanel taskPanel;
    private TaskController taskController;

    private JButton navDashboardButton;
    private JButton navTasksButton;

    /**
     * Constructs the main application window and initializes all primary
     * sub-systems.
     */
    public MainFrame() {
        super("Task Manager Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(MIN_WINDOW_SIZE);
        setResizable(true);

        initServicesAndViews();
        initMenuBar();
        initLayout();

        contentLayout.show(contentArea, VIEW_TASKS);
    }

    private void initServicesAndViews() {
        TaskRepository repository = new TaskRepository();
        TaskServiceImpl taskService = new TaskServiceImpl(repository);
        taskPanel = new TaskPanel();
        taskController = new TaskController(taskService, taskPanel);
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(AppTheme.BG_CARD);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_TABLE));

        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setFont(AppTheme.FONT_BUTTON);
        settingsMenu.setForeground(AppTheme.TEXT_PRIMARY);

        JMenuItem configItem = new JMenuItem("Preferences & Database...");
        configItem.setFont(AppTheme.FONT_BODY);
        configItem.addActionListener(e -> openSettingsDialog());

        settingsMenu.add(configItem);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);
    }

    private void initLayout() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(AppTheme.BG_APP);

        rootPanel.add(createSidebar(), BorderLayout.WEST);

        contentArea.setOpaque(false);
        contentArea.add(createDashboardPlaceholder(), VIEW_DASHBOARD);
        contentArea.add(taskPanel, VIEW_TASKS);

        rootPanel.add(contentArea, BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(AppTheme.BG_CARD);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.BORDER_TABLE));
        sidebar.setPreferredSize(new Dimension(210, 0));

        JLabel brandLabel = new JLabel("Task Manager", SwingConstants.LEFT);
        brandLabel.setFont(AppTheme.FONT_HEADING);
        brandLabel.setForeground(AppTheme.PRIMARY);
        brandLabel.setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));
        sidebar.add(brandLabel);

        navTasksButton = createNavButton("📋  Tasks", true);
        navTasksButton.addActionListener(e -> {
            contentLayout.show(contentArea, VIEW_TASKS);
            updateNavSelection(true);
        });

        navDashboardButton = createNavButton("📊  Dashboard", false);
        navDashboardButton.addActionListener(e -> {
            contentLayout.show(contentArea, VIEW_DASHBOARD);
            updateNavSelection(false);
        });

        JButton navSettingsButton = createNavButton("⚙️  Settings", false);
        navSettingsButton.addActionListener(e -> openSettingsDialog());

        sidebar.add(navTasksButton);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(navDashboardButton);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(navSettingsButton);
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton createNavButton(String text, boolean active) {
        JButton button = new JButton(text);
        button.setFont(AppTheme.FONT_BUTTON);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(190, 40));
        button.setPreferredSize(new Dimension(190, 40));
        button.setAlignmentX(CENTER_ALIGNMENT);
        styleNavButton(button, active);
        return button;
    }

    private void styleNavButton(JButton button, boolean active) {
        if (active) {
            button.setBackground(AppTheme.PRIMARY_MUTED);
            button.setForeground(AppTheme.PRIMARY);
            button.setBorder(BorderFactory.createLineBorder(AppTheme.PRIMARY, 1, true));
        } else {
            button.setBackground(AppTheme.BG_CARD);
            button.setForeground(AppTheme.TEXT_BODY);
            button.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }
    }

    private void updateNavSelection(boolean tasksActive) {
        styleNavButton(navTasksButton, tasksActive);
        styleNavButton(navDashboardButton, !tasksActive);
    }

    private JPanel createDashboardPlaceholder() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACING, AppTheme.SPACING, AppTheme.SPACING,
                AppTheme.SPACING));

        CardPanel card = new CardPanel(new BorderLayout());
        JLabel label = new JLabel("Dashboard Overview (Phase 5 Analytics)", SwingConstants.CENTER);
        label.setFont(AppTheme.FONT_SUBHEADING);
        label.setForeground(AppTheme.TEXT_MUTED);
        card.add(label, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void openSettingsDialog() {
        ConfigurationDialog dialog = new ConfigurationDialog(this);
        dialog.setVisible(true);
        if (taskController != null) {
            taskController.refreshTable();
        }
    }

    public TaskPanel getTaskPanel() {
        return taskPanel;
    }

    public TaskController getTaskController() {
        return taskController;
    }
}
