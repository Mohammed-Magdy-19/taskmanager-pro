package com.taskmanager.model;

import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Task Model Tests")
class TaskTest {

    @Test
    @DisplayName("Default constructor sets proper defaults for priority and status")
    void testDefaultConstructor() {
        Task task = new Task();

        assertEquals(0, task.getId());
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertEquals(Status.PENDING, task.getStatus());
        assertNull(task.getTitle());
        assertNull(task.getDescription());
        assertNull(task.getDueDate());
        assertNull(task.getCategory());
        assertNull(task.getTags());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    @DisplayName("Parameterized constructor populates all fields accurately")
    void testParameterizedConstructor() {
        LocalDateTime due = LocalDateTime.now().plusDays(2);
        Task task = new Task(
                "Write tests",
                "Unit tests for model layer",
                Priority.HIGH,
                Status.IN_PROGRESS,
                due,
                "Development",
                "junit,tdd"
        );

        assertEquals("Write tests", task.getTitle());
        assertEquals("Unit tests for model layer", task.getDescription());
        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals(Status.IN_PROGRESS, task.getStatus());
        assertEquals(due, task.getDueDate());
        assertEquals("Development", task.getCategory());
        assertEquals("junit,tdd", task.getTags());
    }

    @Test
    @DisplayName("Parameterized constructor handles null priority and status with safe defaults")
    void testParameterizedConstructorWithNulls() {
        Task task = new Task("Title", "Desc", null, null, null, null, null);
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertEquals(Status.PENDING, task.getStatus());
    }

    @Test
    @DisplayName("All getters and setters work as expected")
    void testGettersAndSetters() {
        Task task = new Task();
        LocalDateTime due = LocalDateTime.now().plusHours(5);

        task.setId(7);
        task.setTitle("Design review");
        task.setDescription("Review architecture");
        task.setPriority(Priority.LOW);
        task.setStatus(Status.COMPLETED);
        task.setDueDate(due);
        task.setCategory("Architecture");
        task.setTags("review,arch");

        assertEquals(7, task.getId());
        assertEquals("Design review", task.getTitle());
        assertEquals("Review architecture", task.getDescription());
        assertEquals(Priority.LOW, task.getPriority());
        assertEquals(Status.COMPLETED, task.getStatus());
        assertEquals(due, task.getDueDate());
        assertEquals("Architecture", task.getCategory());
        assertEquals("review,arch", task.getTags());
    }

    @Test
    @DisplayName("Task serialization round-trip preservation")
    void testTaskSerialization() throws Exception {
        Task task = new Task("Serialize Task", "Testing serialization", Priority.HIGH,
                Status.IN_PROGRESS, LocalDateTime.now().plusDays(1), "Work", "tag1,tag2");
        task.setId(101);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(task);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            Task deserialized = (Task) ois.readObject();
            assertEquals(task.getId(), deserialized.getId());
            assertEquals(task.getTitle(), deserialized.getTitle());
            assertEquals(task.getDescription(), deserialized.getDescription());
            assertEquals(task.getPriority(), deserialized.getPriority());
            assertEquals(task.getStatus(), deserialized.getStatus());
            assertEquals(task.getDueDate(), deserialized.getDueDate());
            assertEquals(task.getCategory(), deserialized.getCategory());
            assertEquals(task.getTags(), deserialized.getTags());
        }
    }
}
