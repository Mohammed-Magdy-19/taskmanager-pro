package com.taskmanager.repository;

import com.taskmanager.model.User;
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
 * Minimal SQLite implementation of {@link GenericRepository} for {@link User} entities.
 *
 * <p><em>Note: This class is a minimal repository implementation for schema completeness;
 * it remains untested-in-practice until dedicated user authentication/management features
 * are introduced in later phases.</em></p>
 */
public class UserRepository extends GenericRepository<User> {

    private static final AppLogger LOGGER = AppLogger.getLogger(UserRepository.class);

    private static final String INSERT_SQL =
            "INSERT INTO users (username, email, full_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE users SET username = ?, email = ?, full_name = ?, updated_at = ? WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM users WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM users";
    private static final String DELETE_BY_ID_SQL = "DELETE FROM users WHERE id = ?";

    private final DatabaseConnection dbConnection;

    /**
     * Constructs a UserRepository using the default DatabaseConnection.
     */
    public UserRepository() {
        this(DatabaseConnection.getInstance());
    }

    /**
     * Constructs a UserRepository with a specific DatabaseConnection.
     *
     * @param dbConnection the database connection manager
     */
    public UserRepository(DatabaseConnection dbConnection) {
        super(User.class);
        this.dbConnection = Objects.requireNonNull(dbConnection, "dbConnection must not be null");
    }

    @Override
    public User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        synchronized (dbConnection) {
            try {
                return (user.getId() == 0) ? insert(user) : update(user);
            } catch (SQLException e) {
                LOGGER.error("Failed to save user: " + user.getUsername(), e);
                throw new RuntimeException("Database error saving user", e);
            }
        }
    }

    private User insert(User user) throws SQLException {
        user.setUpdatedAt(LocalDateTime.now());
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        Connection conn = dbConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getCreatedAt().toString());
            stmt.setString(5, user.getUpdatedAt().toString());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        }
        return user;
    }

    private User update(User user) throws SQLException {
        user.setUpdatedAt(LocalDateTime.now());
        Connection conn = dbConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getUpdatedAt().toString());
            stmt.setInt(5, user.getId());
            stmt.executeUpdate();
        }
        return user;
    }

    @Override
    public Optional<User> findById(int id) {
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
                LOGGER.error("Failed to find user by id: " + id, e);
                throw new RuntimeException("Database error finding user", e);
            }
            return Optional.empty();
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        users.add(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to find all users", e);
                throw new RuntimeException("Database error finding users", e);
            }
        }
        return users;
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
                LOGGER.error("Failed to delete user with id: " + id, e);
                throw new RuntimeException("Database error deleting user", e);
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        String created = rs.getString("created_at");
        if (created != null) {
            user.setCreatedAt(LocalDateTime.parse(created));
        }
        String updated = rs.getString("updated_at");
        if (updated != null) {
            user.setUpdatedAt(LocalDateTime.parse(updated));
        }
        return user;
    }
}
