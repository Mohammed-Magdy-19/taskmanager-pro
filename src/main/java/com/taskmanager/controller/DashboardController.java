package com.taskmanager.controller;

import com.taskmanager.model.Task;
import com.taskmanager.service.DashboardStats;
import com.taskmanager.service.DashboardStatsCalculator;
import com.taskmanager.service.TaskServiceImpl;
import com.taskmanager.util.AppLogger;

import javax.swing.SwingWorker;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller mediating asynchronous calculation of dashboard statistics
 * from {@link TaskServiceImpl} onto the visual dashboard.
 */
public class DashboardController {

    private static final AppLogger LOGGER = AppLogger.getLogger(DashboardController.class);

    private final TaskServiceImpl taskService;

    public DashboardController(TaskServiceImpl taskService) {
        this.taskService = Objects.requireNonNull(taskService, "taskService must not be null");
    }

    /**
     * Synchronously computes current dashboard statistics.
     *
     * @return calculated {@link DashboardStats}
     */
    public DashboardStats getStats() {
        List<Task> tasks = taskService.findAll();
        return DashboardStatsCalculator.calculate(tasks);
    }

    /**
     * Asynchronously loads dashboard statistics on a background worker thread,
     * delivering results back onto the Swing EDT.
     *
     * @param onSuccess callback invoked on EDT with computed stats
     * @param onError callback invoked on EDT if an exception occurs
     */
    public void loadStats(Consumer<DashboardStats> onSuccess, Consumer<Throwable> onError) {
        SwingWorker<DashboardStats, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardStats doInBackground() {
                List<Task> tasks = taskService.findAll();
                return DashboardStatsCalculator.calculate(tasks);
            }

            @Override
            protected void done() {
                try {
                    DashboardStats stats = get();
                    if (onSuccess != null) {
                        onSuccess.accept(stats);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Failed to load dashboard statistics", ex);
                    if (onError != null) {
                        onError.accept(ex);
                    }
                }
            }
        };
        worker.execute();
    }
}
