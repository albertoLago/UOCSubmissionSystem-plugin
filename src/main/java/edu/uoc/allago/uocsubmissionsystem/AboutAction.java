package edu.uoc.allago.uocsubmissionsystem;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This action triggers the display of the AboutDialog when invoked.
 */
public class AboutAction extends AnAction {

    /**
     * Executes the actual action: displays the AboutDialog.
     *
     * <p>The method is invoked by the IDE when the action is triggered.
     *
     * @param e AnActionEvent.
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AboutDialog dialog = new AboutDialog();
        dialog.show();
    }
}