package com.taskmanager.view;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Empty State Distinct Messaging Logic Tests")
class EmptyStateLogicTest {

    private TaskPanel taskPanel;

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            taskPanel = new TaskPanel();
        });
    }

    @Test
    @DisplayName("Initial empty table with no active filter displays VIEW_EMPTY")
    void testInitialEmptyView() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            taskPanel.getTableModel().setTasks(Collections.emptyList());
            taskPanel.updateViewMode(false);
            assertEquals(TaskPanel.VIEW_EMPTY, taskPanel.getCurrentViewMode());
        });
    }

    @Test
    @DisplayName("Empty table with active search keyword displays VIEW_NO_RESULTS")
    void testEmptyTableWithSearchDisplaysNoResults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            taskPanel.getTableModel().setTasks(Collections.emptyList());
            taskPanel.getSearchField().setText("NonExistentTask");
            taskPanel.updateViewMode();
            assertEquals(TaskPanel.VIEW_NO_RESULTS, taskPanel.getCurrentViewMode());
        });
    }

    @Test
    @DisplayName("Empty table with active filter dropdown displays VIEW_NO_RESULTS")
    void testEmptyTableWithFilterDisplaysNoResults() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            taskPanel.getTableModel().setTasks(Collections.emptyList());
            taskPanel.getFilterComboBox().setSelectedItem("High Priority");
            taskPanel.updateViewMode();
            assertEquals(TaskPanel.VIEW_NO_RESULTS, taskPanel.getCurrentViewMode());
        });
    }

    @Test
    @DisplayName("Table with rows displays VIEW_TABLE regardless of filter state")
    void testTableWithRowsDisplaysTable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Task task = new Task("Sample", "Desc", Priority.MEDIUM, Status.PENDING, null, "Work", "");
            taskPanel.getTableModel().setTasks(Collections.singletonList(task));
            taskPanel.getSearchField().setText("Sample");
            taskPanel.updateViewMode();
            assertEquals(TaskPanel.VIEW_TABLE, taskPanel.getCurrentViewMode());
        });
    }

    @Test
    @DisplayName("isFilterActive detects search text or non-default category")
    void testIsFilterActiveLogic() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            taskPanel.getSearchField().setText("");
            taskPanel.getFilterComboBox().setSelectedItem("All Tasks");
            assertFalse(taskPanel.isFilterActive());

            taskPanel.getSearchField().setText("hello");
            assertTrue(taskPanel.isFilterActive());

            taskPanel.getSearchField().setText("   ");
            assertFalse(taskPanel.isFilterActive());

            taskPanel.getFilterComboBox().setSelectedItem("Completed");
            assertTrue(taskPanel.isFilterActive());
        });
    }
}
