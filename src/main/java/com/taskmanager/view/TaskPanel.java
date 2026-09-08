package com.taskmanager.view;

import com.taskmanager.util.SwingSafe;
import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;
import com.taskmanager.view.theme.TranslucentScrollBarUI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Primary workspace panel presenting the task list, search and filter toolbar,
 * and friendly empty state.
 */
public class TaskPanel extends JPanel {

    private static final String VIEW_TABLE = "TABLE_VIEW";
    private static final String VIEW_EMPTY = "EMPTY_VIEW";

    private final TaskTableModel tableModel;
    private final JTable taskTable;
    private final CardLayout contentCardLayout;
    private final JPanel contentCardPanel;

    private JTextField searchField;
    private JComboBox<String> filterComboBox;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;

    /**
     * Constructs the TaskPanel with styled table, toolbar, and empty view.
     */
    public TaskPanel() {
        super(new BorderLayout(0, AppTheme.SPACING));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACING, AppTheme.SPACING, AppTheme.SPACING, AppTheme.SPACING));

        tableModel = new TaskTableModel();
        taskTable = new JTable();
        tableModel.configureTable(taskTable);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        contentCardLayout = new CardLayout();
        contentCardPanel = new JPanel(contentCardLayout);
        contentCardPanel.setOpaque(false);

        initUI();
        initSelectionListener();
    }

    private void initUI() {
        add(createToolbar(), BorderLayout.NORTH);

        CardPanel tableCard = new CardPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(AppTheme.BG_CARD);
        scrollPane.getVerticalScrollBar().setUI(new TranslucentScrollBarUI());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        contentCardPanel.add(tableCard, VIEW_TABLE);
        contentCardPanel.add(createEmptyStatePanel(), VIEW_EMPTY);

        add(contentCardPanel, BorderLayout.CENTER);
        updateViewMode();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(AppTheme.SPACING, 0));
        toolbar.setOpaque(false);

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACING / 2, 0));
        leftGroup.setOpaque(false);

        searchField = new JTextField(18);
        searchField.setFont(AppTheme.FONT_BODY);
        searchField.putClientProperty("JTextField.placeholderText", "Search tasks by keyword...");
        searchField.setPreferredSize(new Dimension(240, 36));

        String[] filters = {"All Tasks", "Pending", "In Progress", "Completed", "High Priority"};
        filterComboBox = new JComboBox<>(filters);
        filterComboBox.setFont(AppTheme.FONT_BODY);
        filterComboBox.setPreferredSize(new Dimension(140, 36));

        leftGroup.add(searchField);
        leftGroup.add(filterComboBox);

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACING / 2, 0));
        rightGroup.setOpaque(false);

        editButton = new JButton("Edit");
        editButton.setFont(AppTheme.FONT_BUTTON);
        editButton.setEnabled(false);
        editButton.setPreferredSize(new Dimension(80, 36));

        deleteButton = new JButton("Delete");
        deleteButton.setFont(AppTheme.FONT_BUTTON);
        deleteButton.setEnabled(false);
        deleteButton.setPreferredSize(new Dimension(80, 36));

        addButton = new JButton("+ Add Task");
        addButton.setFont(AppTheme.FONT_BUTTON);
        addButton.setBackground(AppTheme.PRIMARY);
        addButton.setForeground(AppTheme.TEXT_INVERTED);
        addButton.setPreferredSize(new Dimension(115, 36));

        rightGroup.add(editButton);
        rightGroup.add(deleteButton);
        rightGroup.add(addButton);

        toolbar.add(leftGroup, BorderLayout.WEST);
        toolbar.add(rightGroup, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createEmptyStatePanel() {
        CardPanel emptyCard = new CardPanel(new BorderLayout());

        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setOpaque(false);

        JLabel titleLabel = new JLabel("No tasks yet — click + Add Task to get started", SwingConstants.CENTER);
        titleLabel.setFont(AppTheme.FONT_SUBHEADING);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("Stay organized, track your goals, and manage your daily priorities.", SwingConstants.CENTER);
        hintLabel.setFont(AppTheme.FONT_BODY);
        hintLabel.setForeground(AppTheme.TEXT_MUTED);
        hintLabel.setAlignmentX(CENTER_ALIGNMENT);

        centerBox.add(Box.createVerticalGlue());
        centerBox.add(titleLabel);
        centerBox.add(Box.createVerticalStrut(AppTheme.SPACING / 2));
        centerBox.add(hintLabel);
        centerBox.add(Box.createVerticalGlue());

        emptyCard.add(centerBox, BorderLayout.CENTER);
        return emptyCard;
    }

    private void initSelectionListener() {
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = taskTable.getSelectedRow() >= 0;
                editButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
            }
        });
    }

    /**
     * Toggles between table view and empty state based on row count.
     */
    public void updateViewMode() {
        SwingSafe.assertEDT();
        if (tableModel.getRowCount() == 0) {
            contentCardLayout.show(contentCardPanel, VIEW_EMPTY);
        } else {
            contentCardLayout.show(contentCardPanel, VIEW_TABLE);
        }
    }

    public TaskTableModel getTableModel() {
        return tableModel;
    }

    public JTable getTaskTable() {
        return taskTable;
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public JComboBox<String> getFilterComboBox() {
        return filterComboBox;
    }

    public JButton getAddButton() {
        return addButton;
    }

    public JButton getEditButton() {
        return editButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }
}
