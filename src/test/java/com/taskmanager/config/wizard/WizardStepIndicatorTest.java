package com.taskmanager.config.wizard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WizardStepIndicatorTest {

    @Test
    void indicatorInitializesNonOpaqueAndAcceptsStepUpdates() {
        WizardStepIndicator indicator = new WizardStepIndicator(5);
        assertFalse(indicator.isOpaque());

        assertDoesNotThrow(() -> indicator.setCurrentStep(0));
        assertDoesNotThrow(() -> indicator.setCurrentStep(2));
        assertDoesNotThrow(() -> indicator.setCurrentStep(4));
        assertDoesNotThrow(() -> indicator.setCurrentStep(-1)); // clamped to 0
        assertDoesNotThrow(() -> indicator.setCurrentStep(10)); // clamped to 4
    }
}
