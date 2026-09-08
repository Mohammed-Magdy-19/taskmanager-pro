package com.taskmanager.config.wizard;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

/**
 * Wizard panel configuring the SQLite database storage location.
 * Allows the user to choose a directory via file chooser or manually enter a path.
 */
public class DatabaseStep extends JPanel implements WizardStepPanel {

    private static final String DEFAULT_DB_FILENAME = "taskmanager.db";
    private final JTextField pathField;

    /**
     * Constructs the DatabaseStep initialized with data from {@link WizardData}.
     *
     * @param data existing wizard settings
     */
    public DatabaseStep(WizardData data) {
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("Database Configuration");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        contentPanel.add(new JLabel("Database File Path:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String initialPath = (data != null && data.getDbPath() != null) ? data.getDbPath() : DEFAULT_DB_FILENAME;
        pathField = new JTextField(initialPath, 24);
        contentPanel.add(pathField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> openDirectoryChooser());
        contentPanel.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        JLabel helpLabel = new JLabel("All application data will be stored locally in this SQLite database file.");
        helpLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
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
