package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Class to handle the action of sending the project to the server.
 * This class extends the ExportAction class.
 */
public class ProjectToServerAction extends ExportAction {

    /**
     * Performs the action of sending the project to the server.
     *
     * @param e The action event.
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Check if the previous action was successful
        if (!previousAction(e)) {
            return;
        }
        if(!validateUser()) return;
        String fileName = userId;

        if (isAdminUser()) {
            fileName = project.getName();
        }

        // Confirm with the user before sending the file
        int result = Messages.showYesNoDialog(
                "Are you sure you want to send the project?",
                "Confirm",
                Messages.getQuestionIcon()
        );

        if (result != Messages.YES) {
            return; // User selected No, so cancel the action
        }

        String filePath =  System.getProperty("java.io.tmpdir")  + "/" + fileName + ".zip";
        File zipFile = new File(filePath);

        // Create a virtual file wrapper for the file
        VirtualFileWrapper virtualFileWrapper = new VirtualFileWrapper(zipFile);
        encryptAndZipProject(virtualFileWrapper);

        // Check if server or poolID are null or blank
        if(isNullOrEmpty(appSettingsState.server) ||  isNullOrEmpty(appSettingsState.poolID)) {
            LOG.warn("Server address or Pool ID cannot be blank or null.");
            // Launch pop-up warning
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showMessageDialog(
                        "Server address or Pool ID cannot be blank or null.",
                        "Warning",
                        Messages.getWarningIcon()
                );
            });
            return;
        }
        // Format server
        appSettingsState.server = removeTrailingSpacesAndSlash(appSettingsState.server);

        // Utiliza HttpClientUploader para enviar el archivo ZIP
        int success = HttpClientUploader.uploadZipFile(zipFile, fileName);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (success==1) {
                Messages.showMessageDialog("Project sent successfully.", "Success", Messages.getInformationIcon());
            }
            if (success==0) {
                Messages.showMessageDialog("Error sending the project.", "Error", Messages.getErrorIcon());
            }
            if (success==2) {
                Messages.showMessageDialog("No response from server.", "Error", Messages.getErrorIcon());
            }
        });
        if (zipFile.exists()) zipFile.delete();
    }

    /**
     * Method to remove trailing whitespaces and slash ("/") from a string.
     *
     * @param str The string from which trailing whitespaces and slash will be removed.
     * @return The string without trailing whitespaces and slash.
     */
    public String removeTrailingSpacesAndSlash(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        // Remove trailing spaces
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.deleteCharAt(sb.length() - 1);
        }
        // Remove trailing slash if it exists
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '/') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
}
