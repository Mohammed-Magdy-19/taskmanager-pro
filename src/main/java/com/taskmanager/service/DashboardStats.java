package com.taskmanager.service;

import java.util.Objects;

/**
 * Immutable value object holding calculated dashboard statistics.
 */
public final class DashboardStats {

    private final int totalCount;
    private final int pendingCount;
    private final int inProgressCount;
    private final int completedCount;
    private final int overdueCount;
    private final double completionPercentage;

    public DashboardStats(int totalCount, int pendingCount, int inProgressCount,
                          int completedCount, int overdueCount, double completionPercentage) {
        this.totalCount = totalCount;
        this.pendingCount = pendingCount;
        this.inProgressCount = inProgressCount;
        this.completedCount = completedCount;
        this.overdueCount = overdueCount;
        this.completionPercentage = completionPercentage;
    }

    public static DashboardStats empty() {
        return new DashboardStats(0, 0, 0, 0, 0, 0.0);
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public int getInProgressCount() {
        return inProgressCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardStats that = (DashboardStats) o;
        return totalCount == that.totalCount &&
                pendingCount == that.pendingCount &&
                inProgressCount == that.inProgressCount &&
                completedCount == that.completedCount &&
                overdueCount == that.overdueCount &&
                Double.compare(that.completionPercentage, completionPercentage) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalCount, pendingCount, inProgressCount, completedCount, overdueCount, completionPercentage);
    }

    @Override
    public String toString() {
        return "DashboardStats{" +
                "totalCount=" + totalCount +
                ", pendingCount=" + pendingCount +
                ", inProgressCount=" + inProgressCount +
                ", completedCount=" + completedCount +
                ", overdueCount=" + overdueCount +
                ", completionPercentage=" + completionPercentage +
                '}';
    }
}
