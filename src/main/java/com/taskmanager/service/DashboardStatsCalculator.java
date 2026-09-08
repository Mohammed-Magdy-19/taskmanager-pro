package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Status;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pure, side-effect-free calculation utility for dashboard metrics.
 */
public final class DashboardStatsCalculator {

    private DashboardStatsCalculator() {
        // Private constructor for utility class
    }

    /**
     * Calculates statistics for the provided tasks relative to the current system time.
     *
     * @param tasks the list of tasks to analyze
     * @return calculated {@link DashboardStats}
     */
    public static DashboardStats calculate(List<Task> tasks) {
        return calculate(tasks, LocalDateTime.now());
    }

    /**
     * Calculates statistics for the provided tasks relative to an explicit reference timestamp.
     *
     * @param tasks the list of tasks to analyze
     * @param referenceTime reference time for determining overdue status
     * @return calculated {@link DashboardStats}
     */
    public static DashboardStats calculate(List<Task> tasks, LocalDateTime referenceTime) {
        if (tasks == null || tasks.isEmpty()) {
            return DashboardStats.empty();
        }

        LocalDateTime now = (referenceTime != null) ? referenceTime : LocalDateTime.now();

        int total = 0;
        int pending = 0;
        int inProgress = 0;
        int completed = 0;
        int overdue = 0;

        for (Task task : tasks) {
            if (task == null) {
                continue;
            }
            total++;
            Status status = task.getStatus();
            if (status == Status.PENDING) {
                pending++;
            } else if (status == Status.IN_PROGRESS) {
                inProgress++;
            } else if (status == Status.COMPLETED) {
                completed++;
            }

            // Completed tasks are NEVER overdue regardless of past due dates
            // Due exactly now is NOT overdue (isBefore semantics)
            if (status != Status.COMPLETED && task.getDueDate() != null) {
                if (task.getDueDate().isBefore(now)) {
                    overdue++;
                }
            }
        }

        double percentage = (total == 0) ? 0.0 : ((double) completed / total) * 100.0;

        return new DashboardStats(total, pending, inProgress, completed, overdue, percentage);
    }
}
