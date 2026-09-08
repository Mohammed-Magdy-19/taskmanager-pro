package com.taskmanager.config.wizard;

/**
 * Contract for wizard and configuration dialog step panels.
 * Ensures each panel can synchronize its UI controls to and from {@link WizardData}.
 */
public interface WizardStepPanel {

    /**
     * Commits the current values from the UI controls into the provided {@link WizardData}.
     *
     * @param data the data holder to update with user input
     */
    void commit(WizardData data);

    /**
     * Refreshes the UI controls based on the current state of {@link WizardData}.
     * Useful when step panels need to echo inputs from earlier steps.
     *
     * @param data the current configuration values
     */
    default void refresh(WizardData data) {
        // Optional hook for steps that require dynamic updates
    }
}
