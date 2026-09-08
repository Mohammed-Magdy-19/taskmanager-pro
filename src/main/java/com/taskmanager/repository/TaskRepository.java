package com.taskmanager.repository;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.util.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite-backed implementation of {@link GenericRepository} for {@link Task} entities.
 * Thread-safe for multi-threaded access, using parameterized queries and a shared row mapper.
 */
public class TaskRepository extends GenericRepository<Task> {

    private static final AppLogger LOGGER = AppLogger.getLogger(TaskRepository.class);

    private static final String INSERT_SQL =
            "INSERT INTO tasks (title, description, priority, status, due_date, category, tags, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_WITH_ID_SQL =
            "INSERT INTO tasks (id, title, description, priority, status, due_date, category, tags, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, due_date = ?, category = ?, "
                    + "tags = ?, updated_at = ? WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM tasks WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM tasks";
    private static final String DELETE_BY_ID_SQL = "DELETE FROM tasks WHERE id = ?";
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM tasks";
    private static final String EXISTS_BY_ID_SQL = "SELECT 1 FROM tasks WHERE id = ? LIMIT 1";
    private static final String FIND_BY_STATUS_SQL = "SELECT * FROM tasks WHERE status = ?";
    private static final String FIND_BY_PRIORITY_SQL = "SELECT * FROM tasks WHERE priority = ?";
    private static final String SEARCH_SQL =
            "SELECT * FROM tasks WHERE title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\'";

    private final DatabaseConnection dbConnection;

    /**
     * Constructs a TaskRepository using the default DatabaseConnection singleton.
     */
    public TaskRepository() {
        this(DatabaseConnection.getInstance());
    }

    /**
     * Constructs a TaskRepository with a specific DatabaseConnection (for testing).
     *
     * @param dbConnection the database connection manager
     */
    public TaskRepository(DatabaseConnection dbConnection) {
        super(Task.class);
        this.dbConnection = Objects.requireNonNull(dbConnection, "dbConnection must not be null");
    }

    @Override
    public Task save(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        synchronized (dbConnection) {
            try {
                return (task.getId() == 0) ? insert(task) : update(task);
            } catch (SQLException e) {
                LOGGER.error("Failed to save task: " + task.getTitle(), e);
                throw new RuntimeException("Database error saving task", e);
            }
        }
    }

    private Task insert(Task task) throws SQLException {
        task.setUpdatedAt(LocalDateTime.now());
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }
        Connection conn = dbConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindInsertParameters(stmt, task);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    task.setId(generatedKeys.getInt(1));
                }
            }
        }
        return task;
    }

    private Task update(Task task) throws SQLException {
        task.setUpdatedAt(LocalDateTime.now());
        Connection conn = dbConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            bindUpdateParameters(stmt, task);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                return insertWithId(task);
            }
        }
        return task;
    }

    private Task insertWithId(Task task) throws SQLException {
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }
        if (task.getUpdatedAt() == null) {
            task.setUpdatedAt(LocalDateTime.now());
        }
        Connection conn = dbConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_WITH_ID_SQL)) {
            bindInsertWithIdParameters(stmt, task);
            stmt.executeUpdate();
        }
        return task;
    }

    private void bindInsertWithIdParameters(PreparedStatement stmt, Task task) throws SQLException {
        stmt.setInt(1, task.getId());
        stmt.setString(2, task.getTitle());
        stmt.setString(3, task.getDescription());
        stmt.setString(4, task.getPriority() != null ? task.getPriority().name() : Priority.MEDIUM.name());
        stmt.setString(5, task.getStatus() != null ? task.getStatus().name() : Status.PENDING.name());
        stmt.setString(6, task.getDueDate() != null ? task.getDueDate().toString() : null);
        stmt.setString(7, task.getCategory());
        stmt.setString(8, task.getTags());
        stmt.setString(9, task.getCreatedAt() != null ? task.getCreatedAt().toString() : LocalDateTime.now().toString());
        stmt.setString(10, task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : LocalDateTime.now().toString());
    }

    @Override
    public Optional<Task> findById(int id) {
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
                    stmt.setInt(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return Optional.of(mapRow(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to find task by id: " + id, e);
                throw new RuntimeException("Database error finding task by id", e);
            }
            return Optional.empty();
        }
    }

    @Override
    public List<Task> findAll() {
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
                     ResultSet rs = stmt.executeQuery()) {
                    return collectRows(rs);
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to retrieve all tasks", e);
                throw new RuntimeException("Database error retrieving tasks", e);
            }
        }
    }

    @Override
    public boolean deleteById(int id) {
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(DELETE_BY_ID_SQL)) {
                    stmt.setInt(1, id);
                    return stmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to delete task with id: " + id, e);
                throw new RuntimeException("Database error deleting task", e);
            }
        }
    }

    @Override
    public int count() {
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(COUNT_SQL);
                     ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to retrieve task count", e);
                throw new RuntimeException("Database error counting tasks", e);
            }
        }
    }

    @Override
    public boolean existsById(int id) {
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(EXISTS_BY_ID_SQL)) {
                    stmt.setInt(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to check task existence by id: " + id, e);
                throw new RuntimeException("Database error checking task existence", e);
            }
        }
    }

    /**
     * Finds tasks filtered by status.
     *
     * @param status the task status
     * @return list of matching tasks
     */
    public List<Task> findByStatus(Status status) {
        if (status == null) {
            return new ArrayList<>();
        }
        return executeQueryWithSingleString(FIND_BY_STATUS_SQL, status.name());
    }

    /**
     * Finds tasks filtered by priority.
     *
     * @param priority the task priority
     * @return list of matching tasks
     */
    public List<Task> findByPriority(Priority priority) {
        if (priority == null) {
            return new ArrayList<>();
        }
        return executeQueryWithSingleString(FIND_BY_PRIORITY_SQL, priority.name());
    }

    /**
     * Searches tasks by keyword matching title or description.
     *
     * @param keyword the search term
     * @return list of unique matching tasks
     */
    public List<Task> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String pattern = "%" + escapeLikePattern(keyword) + "%";
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(SEARCH_SQL)) {
                    stmt.setString(1, pattern);
                    stmt.setString(2, pattern);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return collectRows(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to search tasks with keyword: " + keyword, e);
                throw new RuntimeException("Database error searching tasks", e);
            }
        }
    }

    /**
     * Persists multiple tasks in a single transactional batch.
     * Forward-looking infrastructure for Phase 6 backup restoration.
     *
     * @param tasks the list of tasks to persist
     */
    public void saveAll(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                boolean originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    for (Task task : tasks) {
                        task.setUpdatedAt(LocalDateTime.now());
                        if (task.getCreatedAt() == null) {
                            task.setCreatedAt(LocalDateTime.now());
                        }
                        bindInsertParameters(stmt, task);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                    conn.commit();
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(originalAutoCommit);
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to batch save tasks", e);
                throw new RuntimeException("Database error batch saving tasks", e);
            }
        }
    }

    private List<Task> executeQueryWithSingleString(String sql, String parameter) {
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, parameter);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return collectRows(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to execute query with parameter: " + parameter, e);
                throw new RuntimeException("Database error querying tasks", e);
            }
        }
    }

    private List<Task> collectRows(ResultSet rs) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        while (rs.next()) {
            tasks.add(mapRow(rs));
        }
        return tasks;
    }

    private void bindInsertParameters(PreparedStatement stmt, Task task) throws SQLException {
        stmt.setString(1, task.getTitle());
        stmt.setString(2, task.getDescription());
        stmt.setString(3, task.getPriority() != null ? task.getPriority().name() : Priority.MEDIUM.name());
        stmt.setString(4, task.getStatus() != null ? task.getStatus().name() : Status.PENDING.name());
        stmt.setString(5, task.getDueDate() != null ? task.getDueDate().toString() : null);
        stmt.setString(6, task.getCategory());
        stmt.setString(7, task.getTags());
        stmt.setString(8, task.getCreatedAt() != null ? task.getCreatedAt().toString() : LocalDateTime.now().toString());
        stmt.setString(9, task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : LocalDateTime.now().toString());
    }

    private void bindUpdateParameters(PreparedStatement stmt, Task task) throws SQLException {
        stmt.setString(1, task.getTitle());
        stmt.setString(2, task.getDescription());
        stmt.setString(3, task.getPriority() != null ? task.getPriority().name() : Priority.MEDIUM.name());
        stmt.setString(4, task.getStatus() != null ? task.getStatus().name() : Status.PENDING.name());
        stmt.setString(5, task.getDueDate() != null ? task.getDueDate().toString() : null);
        stmt.setString(6, task.getCategory());
        stmt.setString(7, task.getTags());
        stmt.setString(8, task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : LocalDateTime.now().toString());
        stmt.setInt(9, task.getId());
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getInt("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));

        String priorityStr = rs.getString("priority");
        if (priorityStr != null) {
            task.setPriority(Priority.valueOf(priorityStr));
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            task.setStatus(Status.valueOf(statusStr));
        }

        String dueDateStr = rs.getString("due_date");
        if (dueDateStr != null && !dueDateStr.isEmpty()) {
            task.setDueDate(LocalDateTime.parse(dueDateStr));
        }

        task.setCategory(rs.getString("category"));
        task.setTags(rs.getString("tags"));

        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            task.setCreatedAt(LocalDateTime.parse(createdAtStr));
        }

        String updatedAtStr = rs.getString("updated_at");
        if (updatedAtStr != null && !updatedAtStr.isEmpty()) {
            task.setUpdatedAt(LocalDateTime.parse(updatedAtStr));
        }

        return task;
    }

    private String escapeLikePattern(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
