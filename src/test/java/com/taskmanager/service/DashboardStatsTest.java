package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DashboardStats and DashboardStatsCalculator Tests")
class DashboardStatsTest {

    private final LocalDateTime refTime = LocalDateTime.of(2026, 9, 9, 12, 0, 0);

    @Test
    @DisplayName("Empty or null task list returns all-zero stats")
    void testEmptyAndNullList() {
        DashboardStats emptyStats = DashboardStatsCalculator.calculate(Collections.emptyList(), refTime);
        assertEquals(0, emptyStats.getTotalCount());
        assertEquals(0, emptyStats.getPendingCount());
        assertEquals(0, emptyStats.getInProgressCount());
        assertEquals(0, emptyStats.getCompletedCount());
        assertEquals(0, emptyStats.getOverdueCount());
        assertEquals(0.0, emptyStats.getCompletionPercentage());

        DashboardStats nullStats = DashboardStatsCalculator.calculate(null, refTime);
        assertEquals(emptyStats, nullStats);
    }

    @Test
    @DisplayName("List with null elements skips nulls safely")
    void testListWithNullElements() {
        Task t1 = new Task("T1", "Desc", Priority.MEDIUM, Status.PENDING, null, "Work", "");
        List<Task> tasks = Arrays.asList(t1, null);

        DashboardStats stats = DashboardStatsCalculator.calculate(tasks, refTime);
        assertEquals(1, stats.getTotalCount());
        assertEquals(1, stats.getPendingCount());
        assertEquals(0, stats.getInProgressCount());
        assertEquals(0, stats.getCompletedCount());
        assertEquals(0, stats.getOverdueCount());
        assertEquals(0.0, stats.getCompletionPercentage());
    }

    @Test
    @DisplayName("Status breakdown and percentage calculated correctly")
    void testStatusBreakdownAndPercentage() {
        Task t1 = new Task("T1", "Desc", Priority.HIGH, Status.PENDING, null, "Work", "");
        Task t2 = new Task("T2", "Desc", Priority.LOW, Status.IN_PROGRESS, null, "Work", "");
        Task t3 = new Task("T3", "Desc", Priority.MEDIUM, Status.COMPLETED, null, "Work", "");
        Task t4 = new Task("T4", "Desc", Priority.MEDIUM, Status.COMPLETED, null, "Work", "");

        DashboardStats stats = DashboardStatsCalculator.calculate(Arrays.asList(t1, t2, t3, t4), refTime);
        assertEquals(4, stats.getTotalCount());
        assertEquals(1, stats.getPendingCount());
        assertEquals(1, stats.getInProgressCount());
        assertEquals(2, stats.getCompletedCount());
        assertEquals(0, stats.getOverdueCount());
        assertEquals(50.0, stats.getCompletionPercentage(), 0.001);
    }

    @Test
    @DisplayName("Overdue logic: Completed tasks are never overdue")
    void testCompletedTasksNeverOverdue() {
        LocalDateTime past = refTime.minusDays(2);
        Task completedPastDue = new Task("Done", "Desc", Priority.HIGH, Status.COMPLETED, past, "Work", "");

        DashboardStats stats = DashboardStatsCalculator.calculate(Collections.singletonList(completedPastDue), refTime);
        assertEquals(1, stats.getTotalCount());
        assertEquals(1, stats.getCompletedCount());
        assertEquals(0, stats.getOverdueCount());
        assertEquals(100.0, stats.getCompletionPercentage());
    }

    @Test
    @DisplayName("Overdue logic: Pending and In-Progress past due dates are overdue")
    void testPendingAndInProgressOverdue() {
        LocalDateTime past = refTime.minusMinutes(1);
        Task pendingPast = new Task("P", "Desc", Priority.HIGH, Status.PENDING, past, "Work", "");
        Task inProgressPast = new Task("IP", "Desc", Priority.MEDIUM, Status.IN_PROGRESS, past, "Work", "");

        DashboardStats stats = DashboardStatsCalculator.calculate(Arrays.asList(pendingPast, inProgressPast), refTime);
        assertEquals(2, stats.getTotalCount());
        assertEquals(1, stats.getPendingCount());
        assertEquals(1, stats.getInProgressCount());
        assertEquals(2, stats.getOverdueCount());
    }

    @Test
    @DisplayName("Overdue logic: Tasks due exactly now or in future or null dueDate are not overdue")
    void testDueNowFutureAndNullDueDate() {
        Task dueNow = new Task("Now", "Desc", Priority.HIGH, Status.PENDING, refTime, "Work", "");
        Task dueFuture = new Task("Future", "Desc", Priority.MEDIUM, Status.IN_PROGRESS, refTime.plusDays(1), "Work", "");
        Task noDueDate = new Task("NoDate", "Desc", Priority.LOW, Status.PENDING, null, "Work", "");

        DashboardStats stats = DashboardStatsCalculator.calculate(Arrays.asList(dueNow, dueFuture, noDueDate), refTime);
        assertEquals(3, stats.getTotalCount());
        assertEquals(0, stats.getOverdueCount());
    }

    @Test
    @DisplayName("DashboardStats equals, hashCode, and toString")
    void testValueObjectContract() {
        DashboardStats s1 = new DashboardStats(5, 2, 1, 2, 1, 40.0);
        DashboardStats s2 = new DashboardStats(5, 2, 1, 2, 1, 40.0);
        DashboardStats diff = new DashboardStats(5, 2, 1, 2, 0, 40.0);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, diff);
        assertTrue(s1.toString().contains("totalCount=5"));
    }
}
