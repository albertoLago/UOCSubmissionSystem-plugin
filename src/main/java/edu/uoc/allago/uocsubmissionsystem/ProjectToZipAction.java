package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

/**
 * ProjectToZipAction extends ExportAction for handling the action of zipping and exporting the project.
 */
public class ProjectToZipAction extends ExportAction {


    /**
     * Performs the zipping and exporting action.
     * @param e The event received from the user interface.
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

        // Define zip file extension
        String[] extensions = new String[]{"zip"};

        // Create a file saver descriptor for zip files
        FileSaverDescriptor fsd = new FileSaverDescriptor("Save .zip file",
                "Select output Zip file to save project to.", extensions);

        // Create a file saver dialog
        FileSaverDialog fileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(fsd, project);

        // Save the zip file and store the result
        VirtualFileWrapper result = fileSaverDialog.save(fileName + ".zip");
        if (result == null) {
            return;
        }
        encryptAndZipProject(result);
    }
}
