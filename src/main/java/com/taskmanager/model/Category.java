package com.taskmanager.model;

/**
 * Domain entity representing a category for grouping tasks.
 */
public class Category extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private String color;

    /**
     * Default constructor.
     */
    public Category() {
        super();
    }

    /**
     * Parameterized constructor.
     *
     * @param name the category name
     * @param description the category description
     * @param color the hex or color identifier
     */
    public Category(String name, String description, String color) {
        super();
        this.name = name;
        this.description = description;
        this.color = color;
    }

    /**
     * Gets the category name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the category name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the category description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the category description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the category color representation.
     *
     * @return the color string
     */
    public String getColor() {
        return color;
    }

    /**
     * Sets the category color representation.
     *
     * @param color the color to set
     */
    public void setColor(String color) {
        this.color = color;
    }
}
