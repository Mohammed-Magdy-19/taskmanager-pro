package com.taskmanager.model;

/**
 * Domain entity representing a user in the Task Manager application.
 */
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String username;
    private String email;
    private String fullName;

    /**
     * Default constructor.
     */
    public User() {
        super();
    }

    /**
     * Parameterized constructor.
     *
     * @param username the username
     * @param email the user email address
     * @param fullName the user full name
     */
    public User(String username, String email, String fullName) {
        super();
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the user email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user email.
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the user full name.
     *
     * @param fullName the full name to set
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
