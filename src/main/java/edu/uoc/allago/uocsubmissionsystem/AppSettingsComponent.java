package edu.uoc.allago.uocsubmissionsystem;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class AppSettingsComponent {
    private JPanel contentPane;
    private JTextField passTextField;

    public AppSettingsComponent() {
    }
    
    public JPanel getPanel() {
        return contentPane;
    }

    public JComponent getPreferredFocusedComponent() {
        return passTextField;
    }


    public String getPasswordText() {
        return passTextField.getText();
    }

    public void setPassTextField(String newText) {
        passTextField.setText(newText);
    }

}