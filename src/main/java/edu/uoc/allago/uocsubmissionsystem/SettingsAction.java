package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * When this action is performed, a new settings dialog is created and shown.
 * This class extends {@link AnAction}.
 */
public class SettingsAction extends AnAction {

    /**
     * Creates and shows a new settings dialog when this action is performed.
     *
     * @param e The action event that triggers the dialog.
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        SettingsDialog dialog = new SettingsDialog();
        dialog.show();
    }
}