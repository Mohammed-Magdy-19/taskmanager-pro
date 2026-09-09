package com.taskmanager.service;

import com.taskmanager.event.EventBus;
import com.taskmanager.event.TaskReminderEvent;
import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        EventBus.getInstance().clear();
        notificationService = new NotificationServiceImpl();
    }

    @AfterEach
    void tearDown() {
        if (notificationService != null) {
            notificationService.cleanup();
        }
        EventBus.getInstance().clear();
    }

    @Test
    @DisplayName("Service constructs cleanly and safely handles environment where SystemTray is supported or unsupported")
    void testConstructorSafe() {
        assertDoesNotThrow(() -> {
            NotificationServiceImpl service = new NotificationServiceImpl();
            service.cleanup();
        });
    }

    @Test
    @DisplayName("EventBus delivery: Publishing TaskReminderEvent is consumed without throwing")
    void testEventSubscriptionAndDelivery() {
        Task task = new Task("Urgent Security Patch", "Apply latest security fixes",
                Priority.HIGH, Status.IN_PROGRESS, LocalDateTime.now().plusHours(2), "DevOps", "patch,security");

        assertDoesNotThrow(() -> {
            EventBus.getInstance().publish(new TaskReminderEvent(task));
        });
    }

    @Test
    @DisplayName("showReminder safely handles null task reference")
    void testShowReminderNullTask() {
        assertDoesNotThrow(() -> notificationService.showReminder(null));
    }

    @Test
    @DisplayName("showReminder safely formats and presents task without a due date")
    void testShowReminderWithoutDueDate() {
        Task task = new Task("Ad-hoc Task", "No due date specified", Priority.LOW, Status.PENDING, null, "Personal", null);
        assertDoesNotThrow(() -> notificationService.showReminder(task));
    }

    @Test
    @DisplayName("showReminder safely formats and presents task with valid future due date")
    void testShowReminderWithDueDate() {
        Task task = new Task("Submit Invoice", "Billing department review",
                Priority.MEDIUM, Status.PENDING, LocalDateTime.now().plusDays(1), "Finance", "billing");
        assertDoesNotThrow(() -> notificationService.showReminder(task));
    }

    @Test
    @DisplayName("cleanup handles repeated invocations safely without throwing")
    void testCleanupIdempotent() {
        assertDoesNotThrow(() -> {
            notificationService.cleanup();
            notificationService.cleanup();
        });
    }
}
