package com.taskmanager.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Serializable transport container holding snapshot datasets of tasks and users
 * for backup export and system restoration.
 */
public class BackupData implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Task> tasks;
    private List<User> users;
    private Date timestamp;

    /**
     * Default constructor initializing empty entity collections and current timestamp.
     */
    public BackupData() {
        this.tasks = new ArrayList<>();
        this.users = new ArrayList<>();
        this.timestamp = new Date();
    }

    /**
     * Parameterized constructor for initializing backup data with specific entity datasets.
     *
     * @param tasks list of tasks to backup
     * @param users list of users to backup
     * @param timestamp timestamp of the backup creation
     */
    public BackupData(List<Task> tasks, List<User> users, Date timestamp) {
        this.tasks = (tasks != null) ? new ArrayList<>(tasks) : new ArrayList<>();
        this.users = (users != null) ? new ArrayList<>(users) : new ArrayList<>();
        this.timestamp = (timestamp != null) ? new Date(timestamp.getTime()) : new Date();
    }

    /**
     * Retrieves the list of backed up tasks.
     *
     * @return the list of tasks
     */
    public List<Task> getTasks() {
        return tasks;
    }

    /**
     * Sets the list of backed up tasks.
     *
     * @param tasks the list of tasks to store
     */
    public void setTasks(List<Task> tasks) {
        this.tasks = (tasks != null) ? new ArrayList<>(tasks) : new ArrayList<>();
    }

    /**
     * Retrieves the list of backed up users.
     *
     * @return the list of users
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * Sets the list of backed up users.
     *
     * @param users the list of users to store
     */
    public void setUsers(List<User> users) {
        this.users = (users != null) ? new ArrayList<>(users) : new ArrayList<>();
    }

    /**
     * Retrieves the creation timestamp of the backup.
     *
     * @return the timestamp Date
     */
    public Date getTimestamp() {
        return timestamp != null ? new Date(timestamp.getTime()) : null;
    }

    /**
     * Sets the creation timestamp of the backup.
     *
     * @param timestamp the timestamp Date to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp != null ? new Date(timestamp.getTime()) : null;
    }
}
