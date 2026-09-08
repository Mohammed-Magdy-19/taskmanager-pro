package com.taskmanager.concurrency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadPoolManagerTest {

    @Test
    @DisplayName("submit() executes on a background worker thread different from the calling thread")
    void testSubmitExecutesOnDifferentThread() throws Exception {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        Thread callingThread = Thread.currentThread();

        AtomicReference<Thread> executionThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future = manager.submit(() -> {
            executionThread.set(Thread.currentThread());
            latch.countDown();
        });

        assertNotNull(future);
        boolean finished = latch.await(3, TimeUnit.SECONDS);
        assertTrue(finished, "Task did not complete within timeout");
        assertNotNull(executionThread.get(), "Execution thread was not captured");
        assertNotEquals(callingThread, executionThread.get(), "Task should execute on a different thread");
        assertTrue(executionThread.get().getName().startsWith("task-worker-"), "Thread should use named factory prefix");
    }

    @Test
    @DisplayName("schedule() fires approximately after the configured delay")
    void testScheduleFiresWithApproximateDelay() throws Exception {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        long delayMillis = 150;
        long start = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean fired = new AtomicBoolean(false);

        manager.schedule(() -> {
            fired.set(true);
            latch.countDown();
        }, delayMillis, TimeUnit.MILLISECONDS);

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(finished, "Scheduled task did not fire within timeout");
        assertTrue(fired.get(), "Scheduled task flag was not set");
        // Allow a reasonable tolerance window (at least 110ms and at most 1500ms)
        assertTrue(elapsed >= 110, "Task fired too early: elapsed " + elapsed + " ms");
    }

    @Test
    @DisplayName("50 concurrently submitted tasks all complete exactly once without loss or duplication")
    void testConcurrentSubmissionsCompleteExactlyOnce() throws Exception {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        int taskCount = 50;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < taskCount; i++) {
            manager.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Not all tasks completed within timeout; remaining latch: " + latch.getCount());
        assertEquals(taskCount, counter.get(), "All 50 tasks must be processed exactly once");
    }
}
