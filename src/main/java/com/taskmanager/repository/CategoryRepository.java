package com.taskmanager.repository;

import com.taskmanager.model.Category;
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
 * Minimal SQLite implementation of {@link GenericRepository} for {@link Category} entities.
 *
 * <p><em>Note: This class is a minimal repository implementation for schema completeness;
 * it remains untested-in-practice until dedicated category management features
 * are introduced in later phases.</em></p>
 */
public class CategoryRepository extends GenericRepository<Category> {

    private static final AppLogger LOGGER = AppLogger.getLogger(CategoryRepository.class);

    private static final String INSERT_SQL =
            "INSERT INTO categories (name, description, color, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE categories SET name = ?, description = ?, color = ?, updated_at = ? WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM categories WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM categories";
    private static final String DELETE_BY_ID_SQL = "DELETE FROM categories WHERE id = ?";

    private final DatabaseConnection dbConnection;

    /**
     * Constructs a CategoryRepository using the default DatabaseConnection.
     */
    public CategoryRepository() {
        this(DatabaseConnection.getInstance());
    }

    /**
     * Constructs a CategoryRepository with a specific DatabaseConnection.
     *
     * @param dbConnection the database connection manager
     */
    public CategoryRepository(DatabaseConnection dbConnection) {
        super(Category.class);
        this.dbConnection = Objects.requireNonNull(dbConnection, "dbConnection must not be null");
    }

    @Override
    public Category save(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        synchronized (dbConnection) {
            try {
                return (category.getId() == 0) ? insert(category) : update(category);
            } catch (SQLException e) {
                LOGGER.error("Failed to save category: " + category.getName(), e);
                throw new RuntimeException("Database error saving category", e);
            }
        }
    }

    private Category insert(Category category) throws SQLException {
        category.setUpdatedAt(LocalDateTime.now());
        if (category.getCreatedAt() == null) {
            category.setCreatedAt(LocalDateTime.now());
        }
        Connection conn = dbConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setString(3, category.getColor());
            stmt.setString(4, category.getCreatedAt().toString());
            stmt.setString(5, category.getUpdatedAt().toString());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                }
            }
        }
        return category;
    }

    private Category update(Category category) throws SQLException {
        category.setUpdatedAt(LocalDateTime.now());
        Connection conn = dbConnection.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setString(3, category.getColor());
            stmt.setString(4, category.getUpdatedAt().toString());
            stmt.setInt(5, category.getId());
            stmt.executeUpdate();
        }
        return category;
    }

    @Override
    public Optional<Category> findById(int id) {
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
                LOGGER.error("Failed to find category by id: " + id, e);
                throw new RuntimeException("Database error finding category", e);
            }
            return Optional.empty();
        }
    }

    @Override
    public List<Category> findAll() {
        List<Category> categories = new ArrayList<>();
        synchronized (dbConnection) {
            try {
                Connection conn = dbConnection.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        categories.add(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to find all categories", e);
                throw new RuntimeException("Database error finding categories", e);
            }
        }
        return categories;
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
                LOGGER.error("Failed to delete category with id: " + id, e);
                throw new RuntimeException("Database error deleting category", e);
            }
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getInt("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setColor(rs.getString("color"));
        String created = rs.getString("created_at");
        if (created != null) {
            category.setCreatedAt(LocalDateTime.parse(created));
        }
        String updated = rs.getString("updated_at");
        if (updated != null) {
            category.setUpdatedAt(LocalDateTime.parse(updated));
        }
        return category;
    }
}
