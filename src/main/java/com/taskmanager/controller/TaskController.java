package com.taskmanager.controller;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.service.TaskServiceImpl;
import com.taskmanager.view.TaskFormDialog;
import com.taskmanager.view.TaskPanel;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.util.List;
import java.util.Objects;

/**
 * Controller mediating user interactions between {@link TaskPanel}, {@link TaskFormDialog},
 * and the domain service {@link TaskServiceImpl}.
 */
public class TaskController {

    private final TaskServiceImpl taskService;
    private final TaskPanel taskPanel;

    /**
     * Constructs the TaskController and wires UI event listeners.
     *
     * @param taskService the task service handling persistence and business rules
     * @param taskPanel the visual task panel
     */
    public TaskController(TaskServiceImpl taskService, TaskPanel taskPanel) {
        this.taskService = Objects.requireNonNull(taskService, "taskService must not be null");
        this.taskPanel = Objects.requireNonNull(taskPanel, "taskPanel must not be null");

        initListeners();
        refreshTable();
    }

    private void initListeners() {
        taskPanel.getAddButton().addActionListener(e -> onAddClicked());
        taskPanel.getEditButton().addActionListener(e -> onEditClicked());
        taskPanel.getDeleteButton().addActionListener(e -> onDeleteClicked());

        taskPanel.getSearchField().addActionListener(e -> onSearch(taskPanel.getSearchField().getText()));
        taskPanel.getFilterComboBox().addActionListener(e -> {
            String selected = (String) taskPanel.getFilterComboBox().getSelectedItem();
            onFilterChanged(selected);
        });
    }

    /**
     * Handles the creation of a new task via {@link TaskFormDialog}.
     */
    public void onAddClicked() {
        Window parent = SwingUtilities.getWindowAncestor(taskPanel);
        TaskFormDialog dialog = new TaskFormDialog(parent);
        dialog.setVisible(true);

        if (dialog.isConfirmed() && dialog.getTask() != null) {
            taskService.create(dialog.getTask());
            refreshTable();
        }
    }

    /**
     * Handles editing the selected task via {@link TaskFormDialog}.
     */
    public void onEditClicked() {
        int selectedRow = taskPanel.getTaskTable().getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        int modelRow = taskPanel.getTaskTable().convertRowIndexToModel(selectedRow);
        Task taskToEdit = taskPanel.getTableModel().getTaskAt(modelRow);
        if (taskToEdit == null) {
            return;
        }

        Window parent = SwingUtilities.getWindowAncestor(taskPanel);
        TaskFormDialog dialog = new TaskFormDialog(parent, taskToEdit);
        dialog.setVisible(true);

        if (dialog.isConfirmed() && dialog.getTask() != null) {
            taskService.update(dialog.getTask());
            refreshTable();
        }
    }

    /**
     * Handles deletion of the currently selected task after user confirmation.
     */
    public void onDeleteClicked() {
        int selectedRow = taskPanel.getTaskTable().getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        int modelRow = taskPanel.getTaskTable().convertRowIndexToModel(selectedRow);
        Task taskToDelete = taskPanel.getTableModel().getTaskAt(modelRow);
        if (taskToDelete == null) {
            return;
        }

        Window parent = SwingUtilities.getWindowAncestor(taskPanel);
        int choice = JOptionPane.showConfirmDialog(
                parent,
                "Are you sure you want to delete task: \"" + taskToDelete.getTitle() + "\"?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            taskService.delete(taskToDelete.getId());
            refreshTable();
        }
    }

    /**
     * Filters tasks based on a keyword match across title and description.
     *
     * @param keyword the search query
     */
    public void onSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            refreshTable();
            return;
        }
        List<Task> results = (taskService.getTaskRepository() != null) ?
                taskService.getTaskRepository().searchByKeyword(keyword.trim()) :
                taskService.findAll().stream()
                        .filter(t -> t.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                                (t.getDescription() != null && t.getDescription().toLowerCase().contains(keyword.toLowerCase())))
                        .toList();

        taskPanel.getTableModel().setTasks(results);
        taskPanel.updateViewMode();
    }

    /**
     * Filters tasks according to the chosen category or status from the combo box.
     *
     * @param filter the selected filter name
     */
    public void onFilterChanged(String filter) {
        if (filter == null || "All Tasks".equalsIgnoreCase(filter)) {
            refreshTable();
            return;
        }

        List<Task> results;
        if ("Pending".equalsIgnoreCase(filter)) {
            results = filterByStatus(Status.PENDING);
        } else if ("In Progress".equalsIgnoreCase(filter)) {
            results = filterByStatus(Status.IN_PROGRESS);
        } else if ("Completed".equalsIgnoreCase(filter)) {
            results = filterByStatus(Status.COMPLETED);
        } else if ("High Priority".equalsIgnoreCase(filter)) {
            results = filterByPriority(Priority.HIGH);
        } else {
            results = taskService.findAll();
        }

        taskPanel.getTableModel().setTasks(results);
        taskPanel.updateViewMode();
    }

    private List<Task> filterByStatus(Status status) {
        if (taskService.getTaskRepository() != null) {
            return taskService.getTaskRepository().findByStatus(status);
        }
        return taskService.findAll().stream().filter(t -> t.getStatus() == status).toList();
    }

    private List<Task> filterByPriority(Priority priority) {
        if (taskService.getTaskRepository() != null) {
            return taskService.getTaskRepository().findByPriority(priority);
        }
        return taskService.findAll().stream().filter(t -> t.getPriority() == priority).toList();
    }

    /**
     * Refreshes the table view with the latest tasks from the database.
     */
    public void refreshTable() {
        List<Task> tasks = taskService.findAll();
        taskPanel.getTableModel().setTasks(tasks);
        taskPanel.updateViewMode();
    }
}
