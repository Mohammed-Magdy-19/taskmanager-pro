package com.taskmanager.controller;

import com.taskmanager.concurrency.ThreadPoolManager;
import com.taskmanager.event.EventBus;
import com.taskmanager.event.TaskProcessedEvent;
import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.service.TaskServiceImpl;
import com.taskmanager.util.AppLogger;
import com.taskmanager.view.TaskFormDialog;
import com.taskmanager.view.TaskPanel;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Window;
import java.util.List;
import java.util.Objects;

/**
 * Controller mediating user interactions between {@link TaskPanel}, {@link TaskFormDialog},
 * and the domain service {@link TaskServiceImpl}.
 * Routes all persistence writes through {@link ThreadPoolManager} and subscribes to
 * {@link TaskProcessedEvent} for decoupled UI synchronization.
 */
public class TaskController {

    private static final AppLogger LOGGER = AppLogger.getLogger(TaskController.class);

    private final TaskServiceImpl taskService;
    private final TaskPanel taskPanel;

    /**
     * Constructs the TaskController, wires UI event listeners, and registers
     * the decoupled event bus subscriber for task lifecycle events.
     *
     * @param taskService the task service handling persistence and business rules
     * @param taskPanel the visual task panel
     */
    public TaskController(TaskServiceImpl taskService, TaskPanel taskPanel) {
        this.taskService = Objects.requireNonNull(taskService, "taskService must not be null");
        this.taskPanel = Objects.requireNonNull(taskPanel, "taskPanel must not be null");

        initListeners();
        initEventSubscription();
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

    private void initEventSubscription() {
        EventBus.getInstance().subscribe(TaskProcessedEvent.class, event -> refreshTable());
    }

    /**
     * Handles the creation of a new task via {@link TaskFormDialog}.
     * Executes the persistence operation asynchronously on the shared thread pool
     * and publishes {@link TaskProcessedEvent} upon completion.
     */
    public void onAddClicked() {
        Window parent = SwingUtilities.getWindowAncestor(taskPanel);
        TaskFormDialog dialog = new TaskFormDialog(parent);
        dialog.setVisible(true);

        if (dialog.isConfirmed() && dialog.getTask() != null) {
            final Task draftTask = dialog.getTask();
            ThreadPoolManager.getInstance().submit(() -> {
                try {
                    Task created = taskService.create(draftTask);
                    EventBus.getInstance().publish(new TaskProcessedEvent(created));
                } catch (Exception ex) {
                    LOGGER.error("Failed to create task asynchronously", ex);
                }
            });
        }
    }

    /**
     * Handles editing the selected task via {@link TaskFormDialog}.
     * Executes the update operation asynchronously on the shared thread pool
     * and publishes {@link TaskProcessedEvent} upon completion.
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
            final Task draftTask = dialog.getTask();
            ThreadPoolManager.getInstance().submit(() -> {
                try {
                    Task updated = taskService.update(draftTask);
                    EventBus.getInstance().publish(new TaskProcessedEvent(updated));
                } catch (Exception ex) {
                    LOGGER.error("Failed to update task asynchronously", ex);
                }
            });
        }
    }

    /**
     * Handles deletion of the currently selected task after user confirmation.
     * Executes the deletion asynchronously on the shared thread pool
     * and publishes {@link TaskProcessedEvent} upon completion.
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
            final int taskId = taskToDelete.getId();
            final Task snapshot = taskToDelete.copy();
            ThreadPoolManager.getInstance().submit(() -> {
                try {
                    taskService.delete(taskId);
                    EventBus.getInstance().publish(new TaskProcessedEvent(snapshot));
                } catch (Exception ex) {
                    LOGGER.error("Failed to delete task asynchronously", ex);
                }
            });
        }
    }

    /**
     * Filters tasks based on a keyword query using a background {@link SwingWorker}.
     *
     * @param keyword the search query
     */
    public void onSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            refreshTable();
            return;
        }
        final String query = keyword.trim();
        new SwingWorker<List<Task>, Void>() {
            @Override
            protected List<Task> doInBackground() {
                if (taskService.getTaskRepository() != null) {
                    return taskService.getTaskRepository().searchByKeyword(query);
                }
                return taskService.findAll().stream()
                        .filter(t -> (t.getTitle() != null && t.getTitle().toLowerCase().contains(query.toLowerCase())) ||
                                (t.getDescription() != null && t.getDescription().toLowerCase().contains(query.toLowerCase())))
                        .toList();
            }

            @Override
            protected void done() {
                try {
                    List<Task> results = get();
                    taskPanel.getTableModel().setTasks(results);
                    taskPanel.updateViewMode();
                } catch (Exception ex) {
                    LOGGER.error("Search operation failed for query: " + query, ex);
                }
            }
        }.execute();
    }

    /**
     * Filters tasks according to the chosen category or status using a {@link SwingWorker}.
     *
     * @param filter the selected filter name
     */
    public void onFilterChanged(String filter) {
        if (filter == null || "All Tasks".equalsIgnoreCase(filter)) {
            refreshTable();
            return;
        }

        new SwingWorker<List<Task>, Void>() {
            @Override
            protected List<Task> doInBackground() {
                if ("Pending".equalsIgnoreCase(filter)) {
                    return filterByStatus(Status.PENDING);
                } else if ("In Progress".equalsIgnoreCase(filter)) {
                    return filterByStatus(Status.IN_PROGRESS);
                } else if ("Completed".equalsIgnoreCase(filter)) {
                    return filterByStatus(Status.COMPLETED);
                } else if ("High Priority".equalsIgnoreCase(filter)) {
                    return filterByPriority(Priority.HIGH);
                } else {
                    return taskService.findAll();
                }
            }

            @Override
            protected void done() {
                try {
                    List<Task> results = get();
                    taskPanel.getTableModel().setTasks(results);
                    taskPanel.updateViewMode();
                } catch (Exception ex) {
                    LOGGER.error("Filter operation failed for: " + filter, ex);
                }
            }
        }.execute();
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
     * Refreshes the table view with the latest tasks from the database via {@link SwingWorker}.
     */
    public void refreshTable() {
        new SwingWorker<List<Task>, Void>() {
            @Override
            protected List<Task> doInBackground() {
                return taskService.findAll();
            }

            @Override
            protected void done() {
                try {
                    List<Task> tasks = get();
                    taskPanel.getTableModel().setTasks(tasks);
                    taskPanel.updateViewMode();
                } catch (Exception ex) {
                    LOGGER.error("Failed to refresh task table", ex);
                }
            }
        }.execute();
    }
}
