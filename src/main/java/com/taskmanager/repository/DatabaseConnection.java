package com.taskmanager.repository;

import com.taskmanager.config.ConfigManager;
import com.taskmanager.util.AppLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Thread-safe Singleton managing the SQLite database connection, pragma configuration,
 * and automated idempotent schema execution.
 */
public class DatabaseConnection {

    private static final AppLogger LOGGER = AppLogger.getLogger(DatabaseConnection.class);
    private static final String DEFAULT_DB_PATH = "taskmanager.db";
    private static final String SCHEMA_PATH = "/db/schema.sql";
    private static final int BUSY_TIMEOUT_MS = 5000;

    private static volatile DatabaseConnection instance;

    private final String dbPath;
    private Connection connection;

    /**
     * Constructs a DatabaseConnection instance for the specified database path.
     *
     * @param dbPath the file path to the SQLite database
     */
    DatabaseConnection(String dbPath) {
        this.dbPath = (dbPath != null && !dbPath.trim().isEmpty()) ? dbPath.trim() : DEFAULT_DB_PATH;
    }

    /**
     * Retrieves the Singleton instance using the path configured in {@link ConfigManager}.
     *
     * @return the DatabaseConnection singleton instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            String path = ConfigManager.getInstance().getProperty("db.path");
            instance = new DatabaseConnection(path);
        }
        return instance;
    }

    /**
     * Resets or configures the Singleton instance for a custom database path (useful for testing).
     *
     * @param customDbPath the custom database path
     * @return the newly configured DatabaseConnection instance
     */
    public static synchronized DatabaseConnection resetInstance(String customDbPath) {
        if (instance != null) {
            instance.close();
        }
        instance = new DatabaseConnection(customDbPath);
        return instance;
    }

    /**
     * Retrieves the active database connection, opening a new one and configuring pragmas
     * and schema if the connection is null or was closed.
     *
     * @return an open, configured {@link Connection}
     * @throws SQLException if a database access error occurs
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            openConnection();
        }
        return connection;
    }

    /**
     * Opens a new database connection, applies performance and correctness pragmas,
     * and executes the schema idempotently.
     *
     * @throws SQLException if opening or configuring the connection fails
     */
    private void openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }

        String jdbcUrl = "jdbc:sqlite:" + dbPath;
        connection = DriverManager.getConnection(jdbcUrl);
        applyPragmas(connection);
        executeSchema(connection);
        LOGGER.info("SQLite connection initialized successfully: " + jdbcUrl);
    }

    /**
     * Applies SQLite pragmas to tune concurrency, durability, and integrity.
     *
     * @param conn the active connection
     * @throws SQLException if a SQL error occurs
     */
    private void applyPragmas(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // WAL mode allows concurrent readers during a background writer
            stmt.execute("PRAGMA journal_mode = WAL;");
            // NORMAL synchronous is safe with WAL and provides better throughput
            stmt.execute("PRAGMA synchronous = NORMAL;");
            // Foreign keys must be explicitly enabled in SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");
            // Wait up to 5000ms under lock contention instead of throwing SQLITE_BUSY
            stmt.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS + ";");
        }
    }

    /**
     * Executes the SQL statements from schema.sql on the given connection.
     *
     * @param conn the active connection
     * @throws SQLException if schema execution fails
     */
    public synchronized void executeSchema(Connection conn) throws SQLException {
        String sql = loadSchemaSql();
        String[] statements = sql.split(";");

        try (Statement stmt = conn.createStatement()) {
            for (String rawStatement : statements) {
                String statement = rawStatement.trim();
                if (!statement.isEmpty()) {
                    stmt.execute(statement);
                }
            }
        }
    }

    /**
     * Reads schema.sql from the classpath resources.
     *
     * @return the contents of schema.sql
     * @throws SQLException if the resource cannot be loaded
     */
    private String loadSchemaSql() throws SQLException {
        InputStream stream = getClass().getResourceAsStream(SCHEMA_PATH);
        if (stream == null) {
            throw new SQLException("Database schema file not found at: " + SCHEMA_PATH);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new SQLException("Failed to read schema file: " + SCHEMA_PATH, e);
        }
        return sb.toString();
    }

    /**
     * Closes the underlying database connection if open.
     */
    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.warn("Error closing SQLite connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Gets the database file path currently in use.
     *
     * @return the database path
     */
    public String getDbPath() {
        return dbPath;
    }
}
