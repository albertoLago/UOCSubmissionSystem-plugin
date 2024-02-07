package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;

public class SettingsDialog extends DialogWrapper {

    private JPanel contentPane;
    private JTextField nameField;
    private JTextField userField;
    //private JPasswordField passwordField;
    private JTextField serverField;
    private JTextField poolField;

    public SettingsDialog() {
        // current window as parent
        super(true);
        setTitle("Settings");

        // show stored data
        AppSettingsState appSettingsState = AppSettingsState.getInstance();
        if (appSettingsState != null) {
            nameField.setText(appSettingsState.fullName);
            userField.setText(appSettingsState.userId);
            //passwordField.setText(appSettingsState.retrievePassword(false));
            serverField.setText(appSettingsState.server);
            poolField.setText(appSettingsState.poolID);
        }
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        AppSettingsState appSettingsState = AppSettingsState.getInstance();
        appSettingsState.userId = userField.getText();
        appSettingsState.fullName = nameField.getText();
        appSettingsState.server = serverField.getText();
        appSettingsState.poolID = poolField.getText();

        //char[] password = passwordField.getPassword();
        //appSettingsState.savePassword(userField.getText(), new String(password), false);
        super.doOKAction();
    }
}