package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ProjectToServerDialog extends DialogWrapper {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JProgressBar progressBar1;


    public ProjectToServerDialog() {
        // current window as parent
        super(true);
        setTitle("Project to server ");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        progressBar1.setIndeterminate(false);

        return null;
    }
}
