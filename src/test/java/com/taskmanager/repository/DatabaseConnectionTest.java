package com.taskmanager.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseConnection Tests")
class DatabaseConnectionTest {

    private Path tempDbFile;
    private DatabaseConnection dbConnection;

    @BeforeEach
    void setUp() throws IOException {
        tempDbFile = Files.createTempFile("test_db_", ".db");
        dbConnection = DatabaseConnection.resetInstance(tempDbFile.toAbsolutePath().toString());
    }

    @AfterEach
    void tearDown() {
        if (dbConnection != null) {
            dbConnection.close();
        }
        try {
            Files.deleteIfExists(tempDbFile);
        } catch (IOException ignored) {
        }
    }

    @Test
    @DisplayName("getConnection initializes SQLite schema and creates all tables and indexes")
    void testSchemaInitialization() throws SQLException {
        Connection conn = dbConnection.getConnection();
        assertNotNull(conn);
        assertFalse(conn.isClosed());

        Set<String> tables = new HashSet<>();
        Set<String> indexes = new HashSet<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT type, name FROM sqlite_master")) {
            while (rs.next()) {
                String type = rs.getString("type");
                String name = rs.getString("name");
                if ("table".equalsIgnoreCase(type)) {
                    tables.add(name);
                } else if ("index".equalsIgnoreCase(type)) {
                    indexes.add(name);
                }
            }
        }

        assertTrue(tables.contains("users"), "users table must exist");
        assertTrue(tables.contains("categories"), "categories table must exist");
        assertTrue(tables.contains("tasks"), "tasks table must exist");

        assertTrue(indexes.contains("idx_tasks_status"), "idx_tasks_status must exist");
        assertTrue(indexes.contains("idx_tasks_priority"), "idx_tasks_priority must exist");
        assertTrue(indexes.contains("idx_tasks_due_date"), "idx_tasks_due_date must exist");
        assertTrue(indexes.contains("idx_tasks_category_id"), "idx_tasks_category_id must exist");
        assertTrue(indexes.contains("idx_tasks_status_priority"), "idx_tasks_status_priority must exist");
        assertTrue(indexes.contains("idx_tasks_status_due_date"), "idx_tasks_status_due_date must exist");
    }

    @Test
    @DisplayName("executeSchema is completely idempotent when executed multiple times")
    void testSchemaIdempotency() throws SQLException {
        Connection conn = dbConnection.getConnection();
        assertDoesNotThrow(() -> dbConnection.executeSchema(conn));
        assertDoesNotThrow(() -> dbConnection.executeSchema(conn));
    }

    @Test
    @DisplayName("Pragmas WAL, synchronous, foreign_keys, and busy_timeout are applied correctly")
    void testPragmasApplied() throws SQLException {
        Connection conn = dbConnection.getConnection();

        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("PRAGMA journal_mode;")) {
                assertTrue(rs.next());
                assertEquals("wal", rs.getString(1).toLowerCase());
            }

            try (ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }

            try (ResultSet rs = stmt.executeQuery("PRAGMA busy_timeout;")) {
                assertTrue(rs.next());
                assertEquals(5000, rs.getInt(1));
            }

            try (ResultSet rs = stmt.executeQuery("PRAGMA synchronous;")) {
                assertTrue(rs.next());
                // NORMAL mode in SQLite corresponds to integer value 1
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("getConnection automatically reopens closed connection")
    void testAutomaticReconnection() throws SQLException {
        Connection conn1 = dbConnection.getConnection();
        assertFalse(conn1.isClosed());

        conn1.close();
        assertTrue(conn1.isClosed());

        Connection conn2 = dbConnection.getConnection();
        assertNotNull(conn2);
        assertFalse(conn2.isClosed());
        assertNotSame(conn1, conn2);
    }
}
