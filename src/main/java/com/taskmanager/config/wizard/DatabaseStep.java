package com.taskmanager.config.wizard;

import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

/**
 * Wizard panel configuring the SQLite database storage location.
 * Uses CardPanel container styling and standardized typography.
 */
public class DatabaseStep extends CardPanel implements WizardStepPanel {

    private static final String DEFAULT_DB_FILENAME = "taskmanager.db";
    private final JTextField pathField;

    /**
     * Constructs the DatabaseStep initialized with data from {@link WizardData}.
     *
     * @param data existing wizard settings
     */
    public DatabaseStep(WizardData data) {
        super(new BorderLayout(AppTheme.SPACING, AppTheme.SPACING));

        JLabel titleLabel = new JLabel("Database Storage");
        titleLabel.setFont(AppTheme.FONT_HEADING);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(AppTheme.SPACING / 2, AppTheme.SPACING / 2, AppTheme.SPACING / 2, AppTheme.SPACING / 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel pathLabel = new JLabel("Database File Path:");
        pathLabel.setFont(AppTheme.FONT_BODY_BOLD);
        pathLabel.setForeground(AppTheme.TEXT_PRIMARY);
        contentPanel.add(pathLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String initialPath = (data != null && data.getDbPath() != null) ? data.getDbPath() : DEFAULT_DB_FILENAME;
        pathField = new JTextField(initialPath, 20);
        pathField.setFont(AppTheme.FONT_BODY);
        pathField.setForeground(AppTheme.TEXT_BODY);
        pathField.setCaretColor(AppTheme.PRIMARY);
        contentPanel.add(pathField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseButton = new JButton("Browse...");
        browseButton.setFont(AppTheme.FONT_BUTTON);
        browseButton.setForeground(AppTheme.TEXT_PRIMARY);
        browseButton.addActionListener(e -> openDirectoryChooser());
        contentPanel.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        JLabel helpLabel = new JLabel("All application data will be stored locally in this SQLite database file.");
        helpLabel.setFont(AppTheme.FONT_CAPTION);
        helpLabel.setForeground(AppTheme.TEXT_MUTED);
        contentPanel.add(helpLabel, gbc);

        add(contentPanel, BorderLayout.CENTER);
    }

    private void openDirectoryChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Database Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            String dirPath = chooser.getSelectedFile().getAbsolutePath();
            pathField.setText(dirPath + File.separator + DEFAULT_DB_FILENAME);
        }
    }

    @Override
    public void commit(WizardData data) {
        if (data != null) {
            String text = pathField.getText();
            data.setDbPath((text == null || text.trim().isEmpty()) ? DEFAULT_DB_FILENAME : text.trim());
        }
    }

    @Override
    public void refresh(WizardData data) {
        if (data != null && data.getDbPath() != null) {
            pathField.setText(data.getDbPath());
        }
    }
}
