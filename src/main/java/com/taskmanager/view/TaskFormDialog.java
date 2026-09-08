package com.taskmanager.view;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.validation.TaskValidator;
import com.taskmanager.validation.ValidationResult;
import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Modal dialog for creating and editing tasks with inline field validation.
 */
public class TaskFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Task existingTask;
    private final TaskValidator validator = new TaskValidator();

    private JTextField titleField;
    private JTextArea descriptionArea;
    private JComboBox<Priority> priorityCombo;
    private JComboBox<Status> statusCombo;
    private JTextField dueDateField;
    private JTextField categoryField;
    private JTextField tagsField;

    private JLabel errorTitleLabel;
    private JLabel errorDueDateLabel;

    private Border defaultFieldBorder;
    private boolean confirmed = false;
    private Task resultTask;

    /**
     * Constructs a task form dialog in Add mode.
     *
     * @param owner parent window
     */
    public TaskFormDialog(Window owner) {
        this(owner, null);
    }

    /**
     * Constructs a task form dialog in Edit mode pre-filled with an existing task.
     *
     * @param owner        parent window
     * @param existingTask the task to edit, or null for a new task
     */
    public TaskFormDialog(Window owner, Task existingTask) {
        super(owner, existingTask == null ? "Create New Task" : "Edit Task", ModalityType.APPLICATION_MODAL);
        this.existingTask = existingTask;

        initUI();
        pack();
        setMinimumSize(new Dimension(520, 560));
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        CardPanel contentCard = new CardPanel(new BorderLayout(0, AppTheme.SPACING));

        JLabel headerLabel = new JLabel(existingTask == null ? "Add Task Details" : "Edit Task Details");
        headerLabel.setFont(AppTheme.FONT_SUBHEADING);
        headerLabel.setForeground(AppTheme.TEXT_PRIMARY);
        contentCard.add(headerLabel, BorderLayout.NORTH);

        contentCard.add(createFormPanel(), BorderLayout.CENTER);
        contentCard.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(contentCard);
        if (existingTask != null) {
            populateFields(existingTask);
        }
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        titleField = new JTextField();
        defaultFieldBorder = titleField.getBorder();
        errorTitleLabel = createErrorLabel();

        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setFont(AppTheme.FONT_BODY);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);

        priorityCombo = new JComboBox<>(Priority.values());
        priorityCombo.setSelectedItem(Priority.MEDIUM);

        statusCombo = new JComboBox<>(Status.values());
        statusCombo.setSelectedItem(Status.PENDING);

        dueDateField = new JTextField();
        dueDateField.setToolTipText("Format: yyyy-MM-dd HH:mm (e.g. 2026-10-15 17:00)");
        errorDueDateLabel = createErrorLabel();

        categoryField = new JTextField();
        tagsField = new JTextField();

        int row = 0;
        addFormField(panel, gbc, "Title *", titleField, row++);
        addFieldError(panel, gbc, errorTitleLabel, row++);
        addFormField(panel, gbc, "Description", descScroll, row++);
        addFormField(panel, gbc, "Priority", priorityCombo, row++);
        addFormField(panel, gbc, "Status", statusCombo, row++);
        addFormField(panel, gbc, "Due Date", dueDateField, row++);
        addFieldError(panel, gbc, errorDueDateLabel, row++);
        addFormField(panel, gbc, "Category", categoryField, row++);
        addFormField(panel, gbc, "Tags", tagsField, row);

        return panel;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, String labelText, Component comp, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.25;
        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.FONT_BODY_BOLD);
        label.setForeground(AppTheme.TEXT_BODY);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.75;
        panel.add(comp, gbc);
    }

    private void addFieldError(JPanel panel, GridBagConstraints gbc, JLabel errorLabel, int row) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 0.75;
        panel.add(errorLabel, gbc);
    }

    private JLabel createErrorLabel() {
        JLabel label = new JLabel();
        label.setFont(AppTheme.FONT_CAPTION);
        label.setForeground(AppTheme.DANGER);
        label.setVisible(false);
        return label;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACING, 0));
        panel.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(AppTheme.FONT_BUTTON);
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = new JButton(existingTask == null ? "Save Task" : "Update Task");
        saveButton.setFont(AppTheme.FONT_BUTTON);
        saveButton.setBackground(AppTheme.PRIMARY);
        saveButton.setForeground(AppTheme.TEXT_INVERTED);
        saveButton.addActionListener(e -> onSave());

        panel.add(cancelButton);
        panel.add(saveButton);
        return panel;
    }

    private void populateFields(Task task) {
        titleField.setText(task.getTitle());
        descriptionArea.setText(task.getDescription());
        priorityCombo.setSelectedItem(task.getPriority());
        statusCombo.setSelectedItem(task.getStatus());
        if (task.getDueDate() != null) {
            dueDateField.setText(task.getDueDate().format(DATE_FORMATTER));
        }
        categoryField.setText(task.getCategory());
        tagsField.setText(task.getTags());
    }

    private void onSave() {
        Task draft = (existingTask != null) ? existingTask : new Task();
        draft.setTitle(titleField.getText());
        draft.setDescription(descriptionArea.getText());
        draft.setPriority((Priority) priorityCombo.getSelectedItem());
        draft.setStatus((Status) statusCombo.getSelectedItem());
        draft.setCategory(categoryField.getText());
        draft.setTags(tagsField.getText());

        LocalDateTime parsedDate = parseDueDate();
        draft.setDueDate(parsedDate);

        ValidationResult result = validator.validate(draft);
        if (!result.isValid()) {
            displayErrors(result);
            return;
        }

        clearErrors();
        this.resultTask = draft;
        this.confirmed = true;
        dispose();
    }

    private LocalDateTime parseDueDate() {
        String text = dueDateField.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // If user provided date only, parse with end-of-day time
            try {
                return LocalDateTime.parse(text + " 23:59", DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
                return LocalDateTime.now().minusMinutes(1); // will trigger validator
            }
        }
    }

    private void displayErrors(ValidationResult result) {
        String titleError = result.getError(TaskValidator.FIELD_TITLE);
        if (titleError != null) {
            errorTitleLabel.setText(titleError);
            errorTitleLabel.setVisible(true);
            titleField.setBorder(BorderFactory.createLineBorder(AppTheme.DANGER, 1));
        } else {
            errorTitleLabel.setVisible(false);
            titleField.setBorder(defaultFieldBorder);
        }

        String dueError = result.getError(TaskValidator.FIELD_DUE_DATE);
        if (dueError != null) {
            errorDueDateLabel.setText(dueError);
            errorDueDateLabel.setVisible(true);
            dueDateField.setBorder(BorderFactory.createLineBorder(AppTheme.DANGER, 1));
        } else {
            errorDueDateLabel.setVisible(false);
            dueDateField.setBorder(defaultFieldBorder);
        }
        revalidate();
        repaint();
    }

    private void clearErrors() {
        errorTitleLabel.setVisible(false);
        errorDueDateLabel.setVisible(false);
        titleField.setBorder(defaultFieldBorder);
        dueDateField.setBorder(defaultFieldBorder);
    }

    /**
     * Checks whether the dialog was closed by confirming the save action.
     *
     * @return true if saved, false if cancelled
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Retrieves the validated task resulting from the form submission.
     *
     * @return the task instance
     */
    public Task getTask() {
        return resultTask;
    }
}
