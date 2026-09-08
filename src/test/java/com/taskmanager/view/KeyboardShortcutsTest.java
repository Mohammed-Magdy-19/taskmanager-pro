package com.taskmanager.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MainFrame Keyboard Shortcuts and Modal Guard Tests")
class KeyboardShortcutsTest {

    @Test
    @DisplayName("Ctrl+N and Ctrl+F key strokes are bound to rootPane action map")
    void testKeyBindingsRegistered() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();

            int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            KeyStroke ctrlN = KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask);
            KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcutMask);

            Object bindingN = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ctrlN);
            assertEquals("actionAddNewTask", bindingN);

            Object bindingF = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ctrlF);
            assertEquals("actionFocusSearch", bindingF);

            Action actionN = frame.getRootPane().getActionMap().get("actionAddNewTask");
            assertNotNull(actionN);

            Action actionF = frame.getRootPane().getActionMap().get("actionFocusSearch");
            assertNotNull(actionF);

            frame.dispose();
        });
    }

    @Test
    @DisplayName("isModalDialogOpen detects when an owned modal dialog is active")
    void testModalDialogGuard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            assertFalse(frame.isModalDialogOpen());

            JDialog modalDialog = new JDialog(frame, "Test Modal", Dialog.ModalityType.APPLICATION_MODAL);
            modalDialog.setSize(200, 200);
            modalDialog.setVisible(false);
            assertFalse(frame.isModalDialogOpen());

            // Non-modal dialog should not block
            JDialog nonModalDialog = new JDialog(frame, "Non-Modal", Dialog.ModalityType.MODELESS);
            nonModalDialog.setVisible(true);
            assertFalse(frame.isModalDialogOpen());
            nonModalDialog.dispose();

            modalDialog.dispose();
            frame.dispose();
        });
    }
}
