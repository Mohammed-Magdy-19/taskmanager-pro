package com.taskmanager.concurrency;

import com.taskmanager.util.AppLogger;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton thread pool manager providing centralized asynchronous execution
 * and task scheduling services for the entire application.
 * Ensures all background threads originate from managed pools rather than unmanaged threads.
 */
public final class ThreadPoolManager {

    private static final AppLogger LOGGER = AppLogger.getLogger(ThreadPoolManager.class);

    /** Number of worker threads for general background asynchronous execution and database writes */
    public static final int WORKER_POOL_SIZE = 4;

    /** Number of dedicated scheduler threads for delayed task reminders and recurring timers */
    public static final int SCHEDULER_POOL_SIZE = 2;

    private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();

    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    private ThreadPoolManager() {
        this.executorService = Executors.newFixedThreadPool(
                WORKER_POOL_SIZE,
                new NamedThreadFactory("task-worker")
        );
        this.scheduledExecutorService = Executors.newScheduledThreadPool(
                SCHEDULER_POOL_SIZE,
                new NamedThreadFactory("task-scheduler")
        );
        LOGGER.info("ThreadPoolManager initialized with " + WORKER_POOL_SIZE
                + " worker threads and " + SCHEDULER_POOL_SIZE + " scheduler threads.");
    }

    /**
     * Retrieves the singleton instance of the ThreadPoolManager.
     *
     * @return the singleton ThreadPoolManager
     */
    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }

    /**
     * Submits a Runnable task for asynchronous execution in the shared worker pool.
     *
     * @param task the runnable task to execute
     * @return a Future representing pending completion of the task
     * @throws NullPointerException if task is null
     */
    public Future<?> submit(Runnable task) {
        Objects.requireNonNull(task, "Task to submit cannot be null");
        return executorService.submit(task);
    }

    /**
     * Submits a value-returning Callable task for execution in the shared worker pool.
     *
     * @param <T> the type of the task's result
     * @param task the callable task to execute
     * @return a Future representing pending completion of the task
     * @throws NullPointerException if task is null
     */
    public <T> Future<T> submit(Callable<T> task) {
        Objects.requireNonNull(task, "Callable task cannot be null");
        return executorService.submit(task);
    }

    /**
     * Schedules a one-shot Runnable task for execution after the specified delay.
     *
     * @param command the task to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return a ScheduledFuture representing pending completion of the scheduled task
     * @throws NullPointerException if command or unit is null
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Objects.requireNonNull(command, "Command to schedule cannot be null");
        Objects.requireNonNull(unit, "TimeUnit cannot be null");
        return scheduledExecutorService.schedule(command, delay, unit);
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed,
     * but no new tasks will be accepted.
     */
    public void shutdown() {
        executorService.shutdown();
        scheduledExecutorService.shutdown();
        LOGGER.info("ThreadPoolManager shutdown initiated.");
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return true if all executors terminated, false if timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        boolean workersTerminated = executorService.awaitTermination(timeout, unit);
        boolean schedulerTerminated = scheduledExecutorService.awaitTermination(timeout, unit);
        return workersTerminated && schedulerTerminated;
    }

    /**
     * Checks whether the underlying thread pools have been shut down.
     *
     * @return true if executor service is shut down
     */
    public boolean isShutdown() {
        return executorService.isShutdown() && scheduledExecutorService.isShutdown();
    }

    /**
     * Named thread factory generating descriptive daemon thread names.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger threadCount = new AtomicInteger(1);

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + threadCount.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
