package com.taskmanager.event;

import com.taskmanager.util.AppLogger;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Singleton asynchronous event bus decoupling background publishers from Swing UI subscribers.
 * Automatically guarantees all event deliveries execute on the Swing Event Dispatch Thread (EDT)
 * via {@link SwingUtilities#invokeLater(Runnable)}.
 */
public final class EventBus {

    private static final AppLogger LOGGER = AppLogger.getLogger(EventBus.class);

    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {
        // Singleton
    }

    /**
     * Retrieves the singleton EventBus instance.
     *
     * @return the singleton EventBus
     */
    public static EventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Subscribes a typed consumer callback for events of the specified class.
     *
     * @param <T> the event type
     * @param eventType the class of the event to listen for
     * @param subscriber the consumer invoked on the EDT when the event is published
     * @throws NullPointerException if eventType or subscriber is null
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> subscriber) {
        Objects.requireNonNull(eventType, "Event type class cannot be null");
        Objects.requireNonNull(subscriber, "Subscriber consumer cannot be null");

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((Consumer<Object>) subscriber);
    }

    /**
     * Unsubscribes a previously registered consumer for the specified event class.
     *
     * @param <T> the event type
     * @param eventType the class of the event
     * @param subscriber the consumer to remove
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> subscriber) {
        if (eventType == null || subscriber == null) {
            return;
        }
        List<Consumer<Object>> subscriberList = listeners.get(eventType);
        if (subscriberList != null) {
            subscriberList.remove(subscriber);
            if (subscriberList.isEmpty()) {
                listeners.remove(eventType, subscriberList);
            }
        }
    }

    /**
     * Publishes an event to all registered subscribers.
     * Event delivery is guaranteed to run asynchronously on the Swing Event Dispatch Thread (EDT)
     * using {@link SwingUtilities#invokeLater(Runnable)}.
     *
     * @param event the event instance to publish
     */
    public void publish(Object event) {
        if (event == null) {
            return;
        }

        List<Consumer<Object>> subscriberList = listeners.get(event.getClass());
        if (subscriberList == null || subscriberList.isEmpty()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            for (Consumer<Object> subscriber : subscriberList) {
                try {
                    subscriber.accept(event);
                } catch (Exception ex) {
                    LOGGER.error("Unhandled exception in EventBus subscriber for "
                            + event.getClass().getSimpleName(), ex);
                }
            }
        });
    }

    /**
     * Clears all registered subscribers. Primarily used to reset state between unit tests.
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * Returns the count of registered subscribers for a specific event type.
     *
     * @param eventType the event class
     * @return count of active subscribers
     */
    public int getSubscriberCount(Class<?> eventType) {
        if (eventType == null) {
            return 0;
        }
        List<Consumer<Object>> subscriberList = listeners.get(eventType);
        return subscriberList != null ? subscriberList.size() : 0;
    }
}
