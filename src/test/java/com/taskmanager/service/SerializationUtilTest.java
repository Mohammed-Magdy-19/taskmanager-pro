package com.taskmanager.service;

import com.taskmanager.model.BackupData;
import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationUtilTest {

    private Path tempDir;
    private SerializationUtil<Object> util;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("serialization_test_");
        util = new SerializationUtil<>();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    @DisplayName("Round-trip: serialize and deserialize a simple Serializable POJO preserves deep equality")
    void testSimplePojoRoundTrip() throws Exception {
        String testString = "Hello Task Manager Serialization!";
        String filePath = tempDir.resolve("simple_pojo.ser").toString();

        util.serialize(testString, filePath);
        Object deserialized = util.deserialize(filePath);

        assertEquals(testString, deserialized);
    }

    @Test
    @DisplayName("Round-trip: BackupData with Task containing null dueDate, null category, non-null tags preserves all fields")
    void testBackupDataWithNullAndNonNullFieldsRoundTrip() throws Exception {
        Task task = new Task();
        task.setId(42);
        task.setTitle("Null-edge Task");
        task.setDescription("Detailed description of null-edge task");
        task.setPriority(Priority.HIGH);
        task.setStatus(Status.IN_PROGRESS);
        task.setDueDate(null);
        task.setCategory(null);
        task.setTags("urgent,backend,critical");

        Date now = new Date();
        BackupData originalData = new BackupData(List.of(task), Collections.emptyList(), now);
        String filePath = tempDir.resolve("backup_data.ser").toString();

        util.serialize(originalData, filePath);
        BackupData restored = util.deserialize(filePath, BackupData.class);

        assertNotNull(restored);
        assertNotNull(restored.getTasks());
        assertEquals(1, restored.getTasks().size());

        Task restoredTask = restored.getTasks().get(0);
        assertEquals(42, restoredTask.getId());
        assertEquals("Null-edge Task", restoredTask.getTitle());
        assertEquals("Detailed description of null-edge task", restoredTask.getDescription());
        assertEquals(Priority.HIGH, restoredTask.getPriority());
        assertEquals(Status.IN_PROGRESS, restoredTask.getStatus());
        assertNull(restoredTask.getDueDate(), "dueDate should remain null");
        assertNull(restoredTask.getCategory(), "category should remain null");
        assertEquals("urgent,backend,critical", restoredTask.getTags());
        assertNotNull(restored.getTimestamp());
        assertEquals(now.getTime(), restored.getTimestamp().getTime());
    }

    @Test
    @DisplayName("Round-trip: Empty BackupData deserializes to non-null empty lists without throwing")
    void testEmptyBackupDataRoundTrip() throws Exception {
        Date timestamp = new Date();
        BackupData emptyData = new BackupData(Collections.emptyList(), Collections.emptyList(), timestamp);
        String filePath = tempDir.resolve("empty_backup.ser").toString();

        util.serialize(emptyData, filePath);
        BackupData restored = util.deserialize(filePath, BackupData.class);

        assertNotNull(restored);
        assertNotNull(restored.getTasks(), "tasks list should not be null");
        assertTrue(restored.getTasks().isEmpty(), "tasks list should be empty");
        assertNotNull(restored.getUsers(), "users list should not be null");
        assertTrue(restored.getUsers().isEmpty(), "users list should be empty");
        assertNotNull(restored.getTimestamp());
    }

    @Test
    @DisplayName("deserialize() on a non-existent file throws FileNotFoundException")
    void testDeserializeNonExistentFileThrows() {
        String nonExistentPath = tempDir.resolve("does_not_exist.ser").toString();

        assertThrows(FileNotFoundException.class, () -> {
            util.deserialize(nonExistentPath);
        });
    }

    @Test
    @DisplayName("deserialize() on a file containing corrupted garbage bytes throws StreamCorruptedException")
    void testDeserializeCorruptedGarbageThrows() throws Exception {
        File garbageFile = tempDir.resolve("garbage.ser").toFile();
        try (FileOutputStream fos = new FileOutputStream(garbageFile)) {
            fos.write("THIS IS NOT A VALID JAVA SERIALIZED OBJECT STREAM".getBytes(StandardCharsets.UTF_8));
        }

        assertThrows(StreamCorruptedException.class, () -> {
            util.deserialize(garbageFile.getAbsolutePath());
        });
    }

    @Test
    @DisplayName("deserialize() expecting BackupData on a valid serialized stream of String throws ClassCastException")
    void testDeserializeWrongTypeThrowsClassCastException() throws Exception {
        String stringFile = tempDir.resolve("wrong_type.ser").toString();
        util.serialize("A raw String object, not BackupData", stringFile);

        assertThrows(ClassCastException.class, () -> {
            util.deserialize(stringFile, BackupData.class);
        });
    }
}
