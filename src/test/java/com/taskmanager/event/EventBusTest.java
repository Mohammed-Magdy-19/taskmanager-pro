package com.taskmanager.event;

import com.taskmanager.concurrency.ThreadPoolManager;
import com.taskmanager.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.getInstance();
        eventBus.clear();
    }

    @Test
    @DisplayName("A published event reaches subscriber of its exact type")
    void testPublishedEventReachesSubscriber() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TaskProcessedEvent> received = new AtomicReference<>();

        eventBus.subscribe(TaskProcessedEvent.class, event -> {
            received.set(event);
            latch.countDown();
        });

        Task sample = new Task();
        sample.setId(101);
        sample.setTitle("Test Event");

        eventBus.publish(new TaskProcessedEvent(sample));

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "Event delivery timed out");
        assertNotNull(received.get());
        assertEquals(101, received.get().getTask().getId());
        assertEquals("Test Event", received.get().getTask().getTitle());
    }

    @Test
    @DisplayName("Type isolation: TaskProcessedEvent subscriber never receives TaskReminderEvent")
    void testTypeIsolation() throws Exception {
        CountDownLatch reminderLatch = new CountDownLatch(1);
        AtomicBoolean processedSubscriberInvoked = new AtomicBoolean(false);
        AtomicBoolean reminderSubscriberInvoked = new AtomicBoolean(false);

        eventBus.subscribe(TaskProcessedEvent.class, event -> {
            processedSubscriberInvoked.set(true);
        });

        eventBus.subscribe(TaskReminderEvent.class, event -> {
            reminderSubscriberInvoked.set(true);
            reminderLatch.countDown();
        });

        Task sample = new Task();
        sample.setId(202);
        sample.setTitle("Reminder Task");

        eventBus.publish(new TaskReminderEvent(sample));

        boolean completed = reminderLatch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "Reminder subscriber did not receive event");
        assertTrue(reminderSubscriberInvoked.get());
        assertFalse(processedSubscriberInvoked.get(), "Processed subscriber must not receive reminder event");
    }

    @Test
    @DisplayName("CRITICAL: publish() called from background thread guarantees subscriber runs ON the EDT")
    void testSubscriberRunsOnEDTWhenPublishedFromBackground() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean wasOnEDT = new AtomicBoolean(false);
        AtomicReference<String> callbackThreadName = new AtomicReference<>();

        eventBus.subscribe(TaskProcessedEvent.class, event -> {
            wasOnEDT.set(SwingUtilities.isEventDispatchThread());
            callbackThreadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        Task task = new Task();
        task.setTitle("Background Publish");

        // Publish from ThreadPoolManager background worker thread
        ThreadPoolManager.getInstance().submit(() -> {
            assertFalse(SwingUtilities.isEventDispatchThread(), "Pre-condition: publisher must be off EDT");
            eventBus.publish(new TaskProcessedEvent(task));
        });

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "Event delivery timed out");
        assertTrue(wasOnEDT.get(), "Subscriber callback MUST execute on the EDT! Was on: " + callbackThreadName.get());
    }

    @Test
    @DisplayName("Publishing an event with zero registered subscribers does not throw an exception")
    void testPublishWithNoSubscribersDoesNotThrow() {
        Task task = new Task();
        task.setTitle("Orphan Event");

        assertDoesNotThrow(() -> {
            eventBus.publish(new TaskProcessedEvent(task));
            eventBus.publish(new TaskReminderEvent(task));
        });
    }

    @Test
    @DisplayName("Multiple subscribers registered for the same event type are all invoked")
    void testMultipleSubscribersAllInvoked() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger invocationCount = new AtomicInteger(0);

        eventBus.subscribe(TaskProcessedEvent.class, event -> {
            invocationCount.incrementAndGet();
            latch.countDown();
        });

        eventBus.subscribe(TaskProcessedEvent.class, event -> {
            invocationCount.incrementAndGet();
            latch.countDown();
        });

        eventBus.subscribe(TaskProcessedEvent.class, event -> {
            invocationCount.incrementAndGet();
            latch.countDown();
        });

        Task task = new Task();
        task.setTitle("Multi-Subscriber Task");

        eventBus.publish(new TaskProcessedEvent(task));

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "All subscribers must be invoked within timeout");
        assertEquals(3, invocationCount.get(), "All 3 subscribers must receive the event");
    }
}
