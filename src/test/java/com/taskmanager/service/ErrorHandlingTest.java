package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.repository.GenericRepository;
import com.taskmanager.validation.TaskValidator;
import com.taskmanager.validation.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Error Handling and Edge Case Pass Tests")
class ErrorHandlingTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Blank task title fails validation with clear error message")
    void testBlankTitleValidation() {
        TaskValidator validator = new TaskValidator();
        Task blankTask = new Task("   ", "Description", Priority.MEDIUM, Status.PENDING, null, "Work", "");

        ValidationResult result = validator.validate(blankTask);
        assertFalse(result.isValid());
        assertNotNull(result.getError(TaskValidator.FIELD_TITLE));
        assertTrue(result.getErrors().containsKey(TaskValidator.FIELD_TITLE));
    }

    @Test
    @DisplayName("Corrupt backup file throws IOException during restore")
    void testCorruptedBackupFileThrowsException() throws Exception {
        File corruptFile = tempDir.resolve("corrupt_test.ser").toFile();
        try (FileOutputStream fos = new FileOutputStream(corruptFile)) {
            fos.write(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05}); // Invalid serialized bytes
        }

        BackupManager backupManager = new BackupManager(tempDir);
        assertThrows(IOException.class, () -> {
            backupManager.restoreBackup(corruptFile.getAbsolutePath());
        });
    }

    @Test
    @DisplayName("Updating a non-existent task throws EntityNotFoundException")
    void testUpdateNonExistentTask() {
        GenericRepository<Task> repo = new GenericRepository<>(Task.class);
        TaskServiceImpl service = new TaskServiceImpl(repo);

        Task nonExistent = new Task("Title", "Desc", Priority.LOW, Status.PENDING, null, "Work", "");
        nonExistent.setId(99999);

        assertThrows(EntityNotFoundException.class, () -> {
            service.update(nonExistent);
        });
    }
}
