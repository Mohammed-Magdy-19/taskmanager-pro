package com.taskmanager.config;

import com.taskmanager.config.wizard.DatabaseStep;
import com.taskmanager.config.wizard.PreferencesStep;
import com.taskmanager.config.wizard.ReminderStep;
import com.taskmanager.config.wizard.SummaryStep;
import com.taskmanager.config.wizard.WelcomeStep;
import com.taskmanager.config.wizard.WizardData;
import com.taskmanager.config.wizard.WizardStepIndicator;
import com.taskmanager.config.wizard.WizardStepPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * First-run configuration wizard window.
 * Presents a modern, guided setup flow styled with {@link AppTheme} design tokens,
 * custom step progress indicators, and card containers.
 */
public class ConfigWizardFrame extends JFrame {

    private static final String[] STEP_KEYS = {
            "WELCOME", "DATABASE", "PREFERENCES", "REMINDER", "SUMMARY"
    };
    private static final int FRAME_WIDTH = 560;
    private static final int FRAME_HEIGHT = 420;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final List<WizardStepPanel> stepPanels = new ArrayList<>();
    private final WizardData wizardData = new WizardData();
    private final Runnable onFinish;

    private WizardStepIndicator stepIndicator;
    private int currentStep = 0;

    private JButton backButton;
    private JButton nextButton;
    private JButton finishButton;
    private JButton cancelButton;

    /**
     * Constructs the setup wizard frame.
     *
     * @param onFinish callback invoked after successful configuration completion
     */
    public ConfigWizardFrame(Runnable onFinish) {
        super("Task Manager — First-Run Setup");
        this.onFinish = onFinish;

        initWindow();
        initTopIndicator();
        initSteps();
        initControls();
        updateButtonState();
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setResizable(false);
        setLocationRelativeTo(null);
        getContentPane().setBackground(AppTheme.BG_APP);
        setLayout(new BorderLayout());
    }

    private void initTopIndicator() {
        stepIndicator = new WizardStepIndicator(STEP_KEYS.length);
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setOpaque(false);
        topWrapper.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACING, AppTheme.SPACING * 2, 0, AppTheme.SPACING * 2));
        topWrapper.add(stepIndicator, BorderLayout.CENTER);
        add(topWrapper, BorderLayout.NORTH);
    }

    private void initSteps() {
        cardPanel.setOpaque(false);

        addStepPanel(new WelcomeStep(), STEP_KEYS[0]);
        addStepPanel(new DatabaseStep(wizardData), STEP_KEYS[1]);
        addStepPanel(new PreferencesStep(wizardData), STEP_KEYS[2]);
        addStepPanel(new ReminderStep(wizardData), STEP_KEYS[3]);
        addStepPanel(new SummaryStep(wizardData), STEP_KEYS[4]);

        JPanel cardWrapper = new JPanel(new BorderLayout());
        cardWrapper.setOpaque(false);
        cardWrapper.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACING / 2, AppTheme.SPACING * 2, AppTheme.SPACING, AppTheme.SPACING * 2));
        cardWrapper.add(cardPanel, BorderLayout.CENTER);

        add(cardWrapper, BorderLayout.CENTER);
    }

    private void addStepPanel(JPanel panel, String stepKey) {
        if (panel instanceof WizardStepPanel step) {
            stepPanels.add(step);
        }
        cardPanel.add(panel, stepKey);
    }

    private void initControls() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, AppTheme.SPACING * 2, AppTheme.SPACING * 2, AppTheme.SPACING * 2));

        cancelButton = new JButton("Cancel");
        cancelButton.setFont(AppTheme.FONT_BUTTON);
        cancelButton.setForeground(AppTheme.TEXT_MUTED);
        cancelButton.putClientProperty("JButton.buttonType", "borderless");
        cancelButton.addActionListener(e -> onCancel());
        bottomPanel.add(cancelButton, BorderLayout.WEST);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACING, 0));
        navPanel.setOpaque(false);

        backButton = new JButton("Back");
        backButton.setFont(AppTheme.FONT_BUTTON);
        backButton.setForeground(AppTheme.TEXT_PRIMARY);
        backButton.putClientProperty("JButton.buttonType", "roundRect");
        backButton.addActionListener(e -> onBack());
        navPanel.add(backButton);

        nextButton = new JButton("Next");
        nextButton.setFont(AppTheme.FONT_BUTTON);
        nextButton.setBackground(AppTheme.PRIMARY);
        nextButton.setForeground(AppTheme.TEXT_INVERTED);
        nextButton.putClientProperty("JButton.buttonType", "roundRect");
        nextButton.addActionListener(e -> onNext());
        navPanel.add(nextButton);

        finishButton = new JButton("Finish");
        finishButton.setFont(AppTheme.FONT_BUTTON);
        finishButton.setBackground(AppTheme.PRIMARY);
        finishButton.setForeground(AppTheme.TEXT_INVERTED);
        finishButton.putClientProperty("JButton.buttonType", "roundRect");
        finishButton.addActionListener(e -> onFinishAction());
        finishButton.setVisible(false);
        navPanel.add(finishButton);

        bottomPanel.add(navPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void onNext() {
        commitCurrentStep();
        if (currentStep < STEP_KEYS.length - 1) {
            currentStep++;
            showCurrentStep();
        }
    }

    private void onBack() {
        commitCurrentStep();
        if (currentStep > 0) {
            currentStep--;
            showCurrentStep();
        }
    }

    private void showCurrentStep() {
        stepPanels.get(currentStep).refresh(wizardData);
        stepIndicator.setCurrentStep(currentStep);
        cardLayout.show(cardPanel, STEP_KEYS[currentStep]);
        updateButtonState();
    }

    private void commitCurrentStep() {
        if (currentStep >= 0 && currentStep < stepPanels.size()) {
            stepPanels.get(currentStep).commit(wizardData);
        }
    }

    private void updateButtonState() {
        backButton.setEnabled(currentStep > 0);
        boolean isLastStep = (currentStep == STEP_KEYS.length - 1);
        nextButton.setVisible(!isLastStep);
        finishButton.setVisible(isLastStep);
    }

    private void onFinishAction() {
        commitCurrentStep();
        saveConfiguration();
        dispose();
        if (onFinish != null) {
            onFinish.run();
        }
    }

    private void saveConfiguration() {
        ConfigManager config = ConfigManager.getInstance();
        config.setProperty(ConfigManager.KEY_DB_PATH, wizardData.getDbPath());
        config.setProperty(ConfigManager.KEY_APP_THEME, wizardData.getTheme());
        config.setProperty(ConfigManager.KEY_APP_LANGUAGE, wizardData.getLanguage());
        config.setProperty(ConfigManager.KEY_REMINDER_MINUTES, String.valueOf(wizardData.getReminderMinutes()));
        config.setProperty(ConfigManager.KEY_AUTO_BACKUP, String.valueOf(wizardData.isAutoBackup()));
    }

    private void onCancel() {
        dispose();
        System.exit(0);
    }

    /**
     * Package-private accessor returning the current {@link WizardData}.
     * Useful for automated validation in tests.
     *
     * @return active wizard data
     */
    WizardData getWizardData() {
        return wizardData;
    }

    /**
     * Package-private accessor returning the active step index.
     *
     * @return zero-based step index
     */
    int getCurrentStep() {
        return currentStep;
    }
}
