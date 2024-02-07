package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * This class handles the opening of a project, with operations such as decryption of project files, setting up of
 * listeners and registration of file changes. It utilizes various platform API tools and services and is designed
 * to work specifically with UOC projects.
 */
public class ProjectOpenedManager implements StartupActivity {
    // Flag to control whether the indexing process has finished
    private boolean indexingFinished = false;
    private CipherTools cipherTools;
    private String password;
    private AppSettingsState appSettingsState;
    private String baseDir;
    private int minAdded;
    private String dataFile;
    private static final Logger LOG = Logger.getInstance(ProjectOpenedManager.class);

    /**
     * This method is executed when a project is opened. It checks if the opened project is a UOC project and if so,
     * conducts several actions including decrypting project files, setting up listeners for file changes, reloading
     * the project, logging user actions, and registering listeners for document and file changes.
     *
     * @param project the project being opened
     */
    @Override
    public void runActivity(@NotNull Project project) {

        dataFile = PropertiesLoader.getProperty("dataFile");
        appSettingsState = AppSettingsState.getInstance();

        // Set the project base directory path
        baseDir = project.getBasePath();

        // Check if the project is a UOC project
        File inputFile = new File(baseDir + "/" + dataFile + ".uoc");
        boolean isUOCProject = inputFile.exists();

        if (!LightEdit.owns(project) && isUOCProject) {
            // Add a listener to set the indexingFinished flag to true when the indexing process is finished
            if(PropertiesLoader.getProperty("loggingDelayIndexing").equals("yes")) {
                DumbService.getInstance(project).runWhenSmart(() -> indexingFinished = true);
            } else {
                indexingFinished = true;
            }

            // Load properties from the configuration file
            password = PropertiesLoader.getProperty("key");
            minAdded = PropertiesLoader.getIntProperty("minAdded");

            // Decrypt the project
            cipherTools = new CipherTools();
            int i = cipherTools.decryptProject(baseDir);

            // Decrypt or hide data file
            hideORDecrypt();

            // Reload the project
            if (baseDir != null) {
                VirtualFile projectBaseDir = LocalFileSystem.getInstance().findFileByPath(baseDir);
                if (projectBaseDir != null) {
                    projectBaseDir.refresh(false, true);
                    LOG.info("Project reloaded: " + baseDir);
                }
            }

            // If i > 1, it means there were .uoc files in the opened project (encrypted)
            // so the project is added to a list of projects to be encrypted at closure
            if (i > 1 && !appSettingsState.projects.contains(project.getName())) {
                appSettingsState.projects.add(project.getName());
            }

            // Create a UserActionLogger object to log user actions
            UserActionLogger userActionLogger = new UserActionLogger(project);

            // If the user is not an admin, log the project opening event
            if (!isAdminUser()) {
                userActionLogger.write(true);
                LOG.info("Logged project opening event: " + project.getName());
            }

            // Create a DocumentListener to handle changes in documents
            final DocumentListener documentListener = new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    // Check if the indexing process has finished before logging the modification event
                    if (indexingFinished && !isAdminUser()) {
                        Document document = event.getDocument();
                        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
                        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);

                        // Check if the virtual file belongs to the current project
                        if (virtualFile != null && project.equals(ProjectUtil.guessProjectForFile(virtualFile))) {
                            // Check if the file is a user change
                            if (isUserChange(document, project)) {
                                PsiFile psiFile = psiDocumentManager.getPsiFile(document);

                                // If the modified file is valid, log the modification event if the user is not an admin
                                if (psiFile != null) {
                                    String fileName = psiFile.getName();
                                    String fileNameWithPath = virtualFile.getPath();
                                    int offset = event.getOffset();
                                    int lineNumber = document.getLineNumber(offset);

                                    // Determine if the change size was more than 20 characters
                                    if (event.getNewLength() - event.getOldLength() > minAdded) {
                                        // Get the new text added
                                        String addedCode = event.getNewFragment().toString();

                                        // Call the writeLargeMD method
                                        userActionLogger.writeLargeMD(fileName,fileNameWithPath,lineNumber + 1, addedCode);
                                        LOG.info("Written large modification event to file");
                                    } else {
                                        // Call the writeMD method
                                        userActionLogger.writeMD(fileName,fileNameWithPath, lineNumber + 1);
                                        LOG.info("Written modification event to file");
                                    }
                                }
                            }
                        }
                    }
                }
            };
            // Register the document listener to listen for document changes
            EditorFactory.getInstance().getEventMulticaster().addDocumentListener(documentListener, project);

            LOG.info("Async file listener added to project: " + project.getName());
            // Create an AsyncFileListener to handle file creation and deletion events
            final AsyncFileListener asyncFileListener = new AsyncFileListener() {
                @Nullable
                @Override
                public ChangeApplier prepareChange(@NotNull List<? extends VFileEvent> events) {//polymorphism
                    if (!isAdminUser() && indexingFinished) {
                        // Iterate through the events and handle file deletion and creation events
                        for (VFileEvent event : events) {
                            if (event instanceof VFileDeleteEvent) {
                                String fileName = event.getFile().getName();
                                String fileNameWithPath = event.getPath();
                                userActionLogger.writeCreateDelete(fileName,fileNameWithPath, true);
                                LOG.info("Written delete event to file: " + fileNameWithPath);
                            } else if (event instanceof VFileCreateEvent) {
                                VFileCreateEvent createEvent = (VFileCreateEvent) event;
                                String fileName = createEvent.getChildName();
                                String fileNameWithPath = event.getPath();
                                userActionLogger.writeCreateDelete(fileName,fileNameWithPath, false);
                                LOG.info("Written create event to file: " + fileNameWithPath);
                            }
                        }
                    }
                    return null;
                }
            };
            // Register the async file listener to listen for file deletion and creation events
            VirtualFileManager.getInstance().addAsyncFileListener(asyncFileListener, project);
        }
    }

    /**
     * This method either decrypts or hides data file based on the user type (admin or not).
     */
    private void hideORDecrypt() {
        if (isAdminUser()) {
            cipherTools.decryptFile(baseDir + "/" + dataFile + ".uoc");
        } else {
            // This code prevents ".uoc.data.uoc" from being displayed in the IDE
            ApplicationManager.getApplication().runWriteAction(() -> {
                FileTypeManager fileTypeManager = FileTypeManager.getInstance();
                String ignoreFilesPattern = fileTypeManager.getIgnoredFilesList();
                fileTypeManager.setIgnoredFilesList(ignoreFilesPattern + ";.uoc.data.uoc*");
                LOG.info("File hidden from IDE: .uoc.data.uoc*");
            });

            // Hide dataFile
            Path path = Paths.get(baseDir + "/" + dataFile + ".uoc");
            try {
                Files.setAttribute(path, "dos:hidden", true);
            } catch (IOException e) {
                LOG.error("File not found", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * This method checks if the current user is an admin user.
     * @return true if the user is an admin, false otherwise.
     */
    private boolean isAdminUser() {
        return  password.equals(appSettingsState.retrievePassword(true));
    }

    /**
     * This method checks if the change made in a document is a user change.
     * @param document the document in which change was made.
     * @param project the project where the document belongs.
     * @return true if it's a user change, false otherwise.
     */
    private boolean isUserChange(Document document, Project project) {
        CommandProcessor commandProcessor = CommandProcessor.getInstance();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        return !commandProcessor.isUndoTransparentActionInProgress() && virtualFile != null &&
                !FileStatus.IGNORED.equals(FileStatusManager.getInstance(project).getStatus(virtualFile));
    }
}
