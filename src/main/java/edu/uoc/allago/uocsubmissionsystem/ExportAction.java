package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;

/**
 * Abstract class that provides common functionality for any action that exports data.
 * Derived classes are expected to define what specific export action should be performed.
 */
public abstract class ExportAction extends AnAction {
    protected static final Logger LOG = Logger.getInstance(ExportAction.class);

    protected Project project;
    protected String baseDir;
    protected String userId;
    protected String fullName;
    protected String password;
    protected AppSettingsState appSettingsState;
    protected String dataFile;
    protected String uocTemp;
    private boolean addServerAndPoolID;

    /**
     * Prepares the context for the action to be performed. Initializes project-specific
     * and user-specific fields from the action event and system properties.
     *
     * @param event The action event that triggered this action.
     * @return Returns false if the project is null or AppSettingsState instance is null, true otherwise.
     */
    public boolean previousAction(@NotNull AnActionEvent event) {
        project = event.getProject();
        if(project == null) return false;
        dataFile = PropertiesLoader.getProperty("dataFile");
        uocTemp = System.getProperty("java.io.tmpdir") + "/uoctemp";
        // path to the current project
        baseDir = project.getBasePath();

        // userID and fullName
        appSettingsState = AppSettingsState.getInstance();
        if(appSettingsState == null) {
            return false;
        }
        userId = appSettingsState.userId;
        fullName = appSettingsState.fullName;

        // Load properties from the configuration file
        password = PropertiesLoader.getProperty("key");
        return true;
    }

    /**
     * Encrypts the project and zips it. The method first checks if the user has the necessary
     * permissions, then creates a temporary copy of the project, encrypts user data and project files,
     * zips it and finally deletes the temporary copy.
     *
     * @param virtualFileWrapper The wrapper of the virtual file where the encrypted and zipped project will be saved.
     */
    protected void encryptAndZipProject(VirtualFileWrapper virtualFileWrapper) {

        // Cancel if the project is not from UOC
        if(!isAdminUser()){
            // Check if the project is a UOC project
            File inputFile = new File(baseDir + "/" + dataFile + ".uoc");
            if(!inputFile.exists()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showMessageDialog(
                            "The current project is not from UOC.",
                            "Warning",
                            Messages.getWarningIcon()
                    );
                });
                return;
            }
        }
        // Prepare the project to be exported
        confirmAddServerAndPoolId();

        UserActionLogger userActionLogger = new UserActionLogger(project);
        userActionLogger.writeBufferedEventsToFile();

        DirToZip dirToZip = createTempProjectFolder();

        CipherTools cipherTools = handleUserDataEncryption(uocTemp + "/" + dataFile);

        encryptTempFolder(uocTemp + "/" + dataFile, cipherTools);

        saveEncryptedProjectToZip(dirToZip, virtualFileWrapper);

        deleteTempFolder();
    }

    private void confirmAddServerAndPoolId() {
        if (isAdminUser()) {
            addServerAndPoolID = false;
            int result = Messages.showYesNoDialog(
                    "If a server and pool ID have been entered in the settings, should they be added to the uoc.data file?",
                    "Confirm",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.YES) {
                addServerAndPoolID = true;
            }
        }
    }

    private @NotNull DirToZip createTempProjectFolder() {
        DirToZip dirToZip = new DirToZip();
        try {
            dirToZip.copyToTemp(baseDir);
        } catch (IOException ex) {
            LOG.error("Error when creating a temporary folder containing the project, " +
                    "encryptAndZipProject(VirtualFileWrapper virtualFileWrapper)", ex);
            throw new RuntimeException(ex);
        }
        return dirToZip;
    }

    private @NotNull CipherTools handleUserDataEncryption(String dataFilePath) {
        CipherTools cipherTools = new CipherTools();
        if (!isAdminUser()) {
            cipherTools.decryptFile(dataFilePath + ".uoc");
            setUserData(dataFilePath);

            if (isNullOrEmpty(appSettingsState.server))
                appSettingsState.server = getServerPoolIDValue(dataFilePath, 1);
            if (isNullOrEmpty(appSettingsState.poolID))
                appSettingsState.poolID = getServerPoolIDValue(dataFilePath, 2);
        }
        return cipherTools;
    }

    private void encryptTempFolder(String dataFilePath, @NotNull CipherTools cipherTools) {
        UData();
        cipherTools.encryptFile(dataFilePath);
        cipherTools.encryptProject(uocTemp);
    }

    private void saveEncryptedProjectToZip(@NotNull DirToZip dirToZip,
                                           @NotNull VirtualFileWrapper virtualFileWrapper) {
        try (OutputStream stream = Files.newOutputStream(virtualFileWrapper.getFile().toPath())) {
            dirToZip.excludeStandard();
            dirToZip.zip(uocTemp, stream);
        } catch (IOException ex) {
            LOG.error("Error saving to zip", ex);
        }
    }

    private void deleteTempFolder() {
        try {
            deleteDirectory(uocTemp);
        } catch (IOException ex) {
            LOG.error("Error deleting the folder", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * If the user is an admin, creates an uoc.data file in the temporary folder and encrypts it.
     */
    private void UData() {
        // Cancel if user is not an admin
        if (!isAdminUser()) {
            return;
        }

        Path dataFilePath = Paths.get(uocTemp + "/" + dataFile);
        try {
            // Check if the file exists, if yes, delete it
            if (Files.exists(dataFilePath)) {
                Files.delete(dataFilePath);
            }
            // Create the file
            Files.createFile(dataFilePath);
        } catch (IOException e) {
            LOG.error("Error creating the file", e);
        }

        // Set server and pool info
        if(!isNullOrEmpty(appSettingsState.server) &&
                !isNullOrEmpty(appSettingsState.poolID) &&
                addServerAndPoolID) {
            String NEW_LINE = System.lineSeparator();
            String content= "server:" + appSettingsState.server + NEW_LINE;
            appendToFile(dataFilePath,content);
            content = "poolID:" + appSettingsState.poolID + NEW_LINE;
            appendToFile(dataFilePath,content);
            content = "**********      **********" + NEW_LINE;
            appendToFile(dataFilePath,content);
        }
    }

    /**
     * Deletes the specified directory and its content.
     * @param directoryPath The path of the directory to delete.
     * @throws IOException If an I/O error occurs.
     */
    private void deleteDirectory(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            // Delete a file
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            // Delete a directory after its content has been visited
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (DirectoryNotEmptyException e) {
                    // Ignore the error
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Verifies if the user's credentials are valid.
     *
     * @return Returns true if the user is an admin or has a valid userId and fullName, false otherwise.
     */
    protected boolean validateUser(){
        if (isAdminUser()) return true;

        // Check if the userId is blank or null and show a popup if necessary
        if (isNullOrEmpty(userId) || isNullOrEmpty(fullName)) {
            Messages.showMessageDialog(project,
                    "Please go to settings and enter your user ID and your name.",
                    "User ID and Full Name Required",
                    Messages.getInformationIcon());
            return false;
        }
        return  true;
    }

    /**
     * Appends the user's full name and userId to the uoc.data file.
     *
     * @param strPath The path of the uoc.data file.
     */
    protected void setUserData(String strPath) {
        Path path = Paths.get(strPath);
        String NEW_LINE = System.lineSeparator();
        String content = NEW_LINE + "Name: " + appSettingsState.fullName +
                " - Username: "+appSettingsState.userId + NEW_LINE;
        appendToFile(path, content);
    }

    /**
     * Gets the server or pool ID from a specified line in a file.
     *
     * @param filePath   The path to the file.
     * @param lineNumber The number of the line to read from.
     * @return The server or pool ID from the specified line, or null if an error occurs.
     */
    protected String getServerPoolIDValue(String filePath, int lineNumber) {
        File file = new File(filePath);
        int currentLine = 1;

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (currentLine == lineNumber) {
                    if (line.startsWith("poolID:")) {
                        return line.split("poolID:")[1].trim();
                    } else if (line.startsWith("server:")) {
                        return line.split("server:")[1].trim();
                    } else {
                        LOG.error("The specified line does not start with 'poolID:' or 'server:'");
                        return null;
                    }
                }
                currentLine++;
            }
        } catch (FileNotFoundException e) {
            LOG.error("File not found: " + filePath, e);
        }
        LOG.error("The specified line number exceeds the number of lines in the file.");
        return null;
    }

    /**
     * Appends a string to the end of a file.
     *
     * @param path    The path of the file.
     * @param content The string to be appended to the file.
     */
    protected void appendToFile(Path path, @NotNull String content)   {
        try {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Error writing to uoc.data", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Determines if the current user has admin privileges.
     *
     * @return true if the user is an admin, false otherwise.
     */
    protected boolean isAdminUser() {
        if (password.equals(appSettingsState.retrievePassword(true))) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a given string is null or empty after trimming whitespace.
     *
     * @param str the string to check; may be null
     * @return true if the string is null, or if the string is empty after trimming whitespace; false otherwise
     */
    protected boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}