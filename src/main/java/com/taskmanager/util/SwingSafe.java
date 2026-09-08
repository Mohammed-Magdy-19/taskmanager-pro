package com.taskmanager.util;

import javax.swing.SwingUtilities;

/**
 * Utility helper for defensive EDT safety and assertion checking.
 * Guarantees UI modifications execute on the Event Dispatch Thread (EDT)
 * and provides runtime assertions in development builds.
 */
public final class SwingSafe {

    private SwingSafe() {
        // Utility class
    }

    /**
     * Asserts that the current thread is the Swing Event Dispatch Thread (EDT).
     * Meaningful when Java assertions are enabled with the {@code -ea} JVM flag.
     *
     * @throws AssertionError if invoked from a non-EDT background thread when assertions are enabled
     */
    public static void assertEDT() {
        assert SwingUtilities.isEventDispatchThread() : "Not on EDT! Called from thread: " + Thread.currentThread().getName();
    }

    /**
     * Executes the given task on the Swing Event Dispatch Thread (EDT).
     * Runs synchronously if already on the EDT; otherwise schedules asynchronously
     * via {@link SwingUtilities#invokeLater(Runnable)}.
     *
     * @param action the task to execute
     */
    public static void run(Runnable action) {
        if (action == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
