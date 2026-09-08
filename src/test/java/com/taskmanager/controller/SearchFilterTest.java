package com.taskmanager.controller;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.repository.GenericRepository;
import com.taskmanager.service.TaskServiceImpl;
import com.taskmanager.view.TaskPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Search, Filter, and Race Condition Tests")
class SearchFilterTest {

    private GenericRepository<Task> genericRepo;
    private TaskServiceImpl taskService;
    private TaskPanel taskPanel;
    private TaskController taskController;

    @BeforeEach
    void setUp() throws Exception {
        genericRepo = new GenericRepository<>(Task.class);
        taskService = new TaskServiceImpl(genericRepo);

        Task t1 = new Task("Review PR", "Code review for authentication", Priority.HIGH, Status.PENDING, null, "Work", "dev");
        Task t2 = new Task("Write Documentation", "Project guidelines and readme", Priority.MEDIUM, Status.IN_PROGRESS, null, "Work", "docs");
        Task t3 = new Task("Fix Bug", "Null pointer in report export", Priority.LOW, Status.COMPLETED, null, "Work", "bug");
        taskService.create(t1);
        taskService.create(t2);
        taskService.create(t3);

        SwingUtilities.invokeAndWait(() -> {
            taskPanel = new TaskPanel();
            taskController = new TaskController(taskService, taskPanel);
        });
    }

    @Test
    @DisplayName("Search sequence increments on each search and filter call")
    void testSearchSequenceIncrements() throws Exception {
        long initialSeq = taskController.getCurrentSearchSequence();

        SwingUtilities.invokeAndWait(() -> taskController.onSearch("Review"));
        assertTrue(taskController.getCurrentSearchSequence() > initialSeq);

        long afterSearch = taskController.getCurrentSearchSequence();
        SwingUtilities.invokeAndWait(() -> taskController.onFilterChanged("Pending"));
        assertTrue(taskController.getCurrentSearchSequence() > afterSearch);
    }

    @Test
    @DisplayName("Search by keyword finds matching title or description (case-insensitive)")
    void testSearchByKeyword() throws Exception {
        SwingUtilities.invokeAndWait(() -> taskController.onSearch("authentication"));
        waitForEdt();

        assertEquals(1, taskPanel.getTableModel().getRowCount());
        assertEquals("Review PR", taskPanel.getTableModel().getTaskAt(0).getTitle());

        SwingUtilities.invokeAndWait(() -> taskController.onSearch("WRITE"));
        waitForEdt();

        assertEquals(1, taskPanel.getTableModel().getRowCount());
        assertEquals("Write Documentation", taskPanel.getTableModel().getTaskAt(0).getTitle());
    }

    @Test
    @DisplayName("Filter by Status populates matching tasks")
    void testFilterByStatus() throws Exception {
        SwingUtilities.invokeAndWait(() -> taskController.onFilterChanged("Completed"));
        waitForEdt();

        assertEquals(1, taskPanel.getTableModel().getRowCount());
        assertEquals(Status.COMPLETED, taskPanel.getTableModel().getTaskAt(0).getStatus());
    }

    @Test
    @DisplayName("Filter by Priority populates matching tasks")
    void testFilterByPriority() throws Exception {
        SwingUtilities.invokeAndWait(() -> taskController.onFilterChanged("High Priority"));
        waitForEdt();

        assertEquals(1, taskPanel.getTableModel().getRowCount());
        assertEquals(Priority.HIGH, taskPanel.getTableModel().getTaskAt(0).getPriority());
    }

    @Test
    @DisplayName("Empty or whitespace search resets to full list")
    void testEmptySearchResets() throws Exception {
        SwingUtilities.invokeAndWait(() -> taskController.onSearch("NonExistentQuery12345"));
        waitForEdt();
        assertEquals(0, taskPanel.getTableModel().getRowCount());

        SwingUtilities.invokeAndWait(() -> taskController.onSearch("   "));
        waitForEdt();
        assertEquals(3, taskPanel.getTableModel().getRowCount());
    }

    private void waitForEdt() throws InterruptedException, InvocationTargetException {
        Thread.sleep(100);
        SwingUtilities.invokeAndWait(() -> {});
    }
}
