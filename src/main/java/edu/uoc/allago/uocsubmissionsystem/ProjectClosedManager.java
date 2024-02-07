package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Implements a project listener for IntelliJ IDEA plugins that handles the
 * encryption and decryption of project files upon opening and closing a project.
 * It also logs closing actions if the user is not an admin.
 */
public class ProjectClosedManager implements com.intellij.openapi.project.ProjectManagerListener {

    private final AppSettingsState appSettingsState = AppSettingsState.getInstance();

    private static final Logger LOG = Logger.getInstance(ProjectClosedManager.class);

    /**
     * Handles project closing actions, such as encrypting the project files.
     *
     * @param project the project being closed
     */
    @Override
    public void projectClosed(@NotNull Project project) {
        String dataFile = PropertiesLoader.getProperty("dataFile");
        String baseDir = project.getBasePath();

        // Check if the project is a UOC project
        File inputFile = new File(baseDir + "/" + dataFile + ".uoc");
        boolean isUOCProject = inputFile.exists();

        if (!LightEdit.owns(project) && isUOCProject) {
            CipherTools cipherTools = new CipherTools();
            // If the project is in the list of projects
            if (appSettingsState.projects.contains(project.getName())) {
                appSettingsState.projects.removeIf(s -> s.equals(project.getName()));
            }
            // If the user is not an admin
            UserActionLogger userActionLogger = new UserActionLogger(project);
            if (!isAdminUser()) {
                cipherTools.encryptProject(baseDir);
                userActionLogger.write(false);

                LOG.info("Written user actions to file");
                userActionLogger.writeBufferedEventsToFile();
                userActionLogger.stop();

            } else {
                LOG.info("Decrypting file: " + baseDir + "/" + dataFile + ".uoc");
                cipherTools.decryptFile(baseDir + "/" + dataFile + ".uoc");
            }
        }
    }

    /**
     * This method checks if the current user is an admin user.
     * @return true if the user is an admin, false otherwise.
     */
    private boolean isAdminUser() {
        return PropertiesLoader.getProperty("key").equals(appSettingsState.retrievePassword(true));
    }
}