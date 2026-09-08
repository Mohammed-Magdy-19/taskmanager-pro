package com.taskmanager.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base entity providing common identity and auditing properties.
 * Implements {@link Serializable} with a fixed serialVersionUID.
 * Identity equality is evaluated solely on the numeric identifier {@code id}.
 */
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    protected int id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    /**
     * Default constructor initializing creation and last-updated timestamps to current time.
     */
    public BaseEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Retrieves the entity primary key identifier.
     *
     * @return the integer id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the entity primary key identifier.
     *
     * @param id the integer id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Retrieves the timestamp when this entity was created.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when this entity was created.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Retrieves the timestamp when this entity was last updated.
     *
     * @return the last updated timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp when this entity was last updated.
     *
     * @param updatedAt the last updated timestamp
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
