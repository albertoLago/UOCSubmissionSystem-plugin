package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserActionLogger {
    private static final Logger LOG = Logger.getInstance(UserActionLogger.class);
    private static final String NEW_LINE = System.lineSeparator();
    private final String baseDir;
    private String lastFileMD;
    private final boolean isUOCProject;
    private final CipherTools cipherTools;
    private final String dataFile;
    private final int minTimeBetweenLogs;
    private final AtomicBoolean blockWriteMD = new AtomicBoolean(false);
    private final AtomicBoolean blockWriteTime = new AtomicBoolean(false);
    private LocalDateTime lastOpenedTime;
    private final int delayInSeconds;
    private final List<String> relevantExtensions;
    private final List<String> relevantSequences;
    private static ScheduledExecutorService scheduler;
    private static final List<String> eventBuffer = Collections.synchronizedList(new ArrayList<>());

    /**
     * Constructor for UserActionLogger, initializes values and loads properties.
     *
     * @param project The current project.
     */
    public UserActionLogger(@NotNull Project project) {
        dataFile = PropertiesLoader.getProperty("dataFile");
        lastFileMD = "";
        cipherTools = new CipherTools();
        baseDir = project.getBasePath();

        File inputFile = new File(baseDir + "/" + dataFile + ".uoc");
        isUOCProject = inputFile.exists();

        minTimeBetweenLogs = PropertiesLoader.getIntProperty("minTimeBetweenLogs");
        delayInSeconds = PropertiesLoader.getIntProperty("delayInSeconds");
        String relevantExtensionsProperty = PropertiesLoader.getProperty("relevantExtensions");
        if (relevantExtensionsProperty != null && !relevantExtensionsProperty.isEmpty()) {
            relevantExtensions = Arrays.asList(relevantExtensionsProperty.split(","));
        } else {
            relevantExtensions = Collections.emptyList();
        }
        String relevantSequencesProperty = PropertiesLoader.getProperty("relevantSequences");
        if (relevantSequencesProperty != null && !relevantSequencesProperty.isEmpty()) {
            relevantSequences = Arrays.asList(relevantSequencesProperty.split(","));
        } else {
            relevantSequences = Collections.emptyList();
        }

        // Time between writes to the hard disk
        int period = PropertiesLoader.getIntProperty("eventsToFilePeriod");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> writeBufferedEventsToFile(), period, period, TimeUnit.MINUTES);
    }

    /**
     * Writes the user action (open or close) to the eventBuffer.
     *
     * @param opened True if the action is open, false if close.
     */
    public void write(Boolean opened) {
        if (!isUOCProject) return;
        String content = opened ? "OP" : "CL";
        addToBuffer(content + time() + NEW_LINE);
        if (opened) {
            lastOpenedTime = LocalDateTime.now();
        }
    }

    /**
     * Writes the modified file name, timestamp and potentially line number to the eventBuffer.
     *
     * @param fileMD     The modified file name.
     * @param fileNameWithPath The modified file path.
     * @param lineNumber The line number where the code was changed, or 0 if the file was deleted.
     */
    public void writeMD(String fileMD, String fileNameWithPath, int lineNumber) {
        if (!isUOCProject || shouldSkip(fileNameWithPath) || lineNumber == 1 || blockWriteMD.get()) return;
        if (lastFileMD.equals(fileMD) && blockWriteTime.get()) return;

        // Save last logged file
        lastFileMD = fileMD;
        LOG.info("Writing to uoc.data, edited file: " + fileMD);

        // Unblock after minTimeBetweenLogs seconds
        blockWriteTime.set(true);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> blockWriteTime.set(false), minTimeBetweenLogs, TimeUnit.SECONDS);
        addToBuffer("MD" + time() + "Line: " +
                lineNumber + "     File: " + fileMD + NEW_LINE);
    }

    /**
     * Writes the modified file name, timestamp, line number and added code to the eventBuffer.
     *
     * @param fileMD     The modified file name.
     * @param fileNameWithPath The modified file path.
     * @param lineNumber The line number where the code was added.
     * @param addedCode  The code that was added.
     */
    public void writeLargeMD(String fileMD,String fileNameWithPath, int lineNumber, String addedCode) {
        if (!isUOCProject || shouldSkip(fileNameWithPath) || shouldSkipCode(addedCode) || blockWriteMD.get()) return;

        // Save last logged file
        lastFileMD = fileMD;
        addToBuffer("MD" + time() + "Line: " +
                lineNumber + "     File: " + fileMD + "     PASTED CODE:" + NEW_LINE + NEW_LINE +
                addedCode + NEW_LINE + NEW_LINE);
    }

    /**
     * Logs the creation or deletion of a file in the project.
     * <p>
     * If the file is part of a UOC project and does not meet the skip criteria,
     * this method logs the file creation or deletion event with a timestamp.
     *
     * @param fileMD The name of the file being created or deleted.
     * @param fileNameWithPath The path of the file being created or deleted.
     * @param delete A Boolean flag indicating whether the file is being deleted.
     *               If true, the file is being deleted.
     *               If false, the file is being created.
     */
    public void writeCreateDelete(String fileMD, String fileNameWithPath, Boolean delete) {
        if (!isUOCProject || shouldSkip(fileNameWithPath)) return;
        LOG.info("Writing to uoc.data, edited file: " + fileMD);

        if (Duration.between(lastOpenedTime, LocalDateTime.now()).getSeconds() < delayInSeconds) {
            return;
        }
        if (delete) {
            addToBuffer("DL" + time() + " File: " + fileMD + NEW_LINE);
        } else {
            addToBuffer("CR" + time() + " File: " + fileMD + NEW_LINE);

            // Suspend writeMD and writeLargeMD
            blockWriteMD.set(true);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> blockWriteMD.set(false), 3, TimeUnit.SECONDS);
        }
    }

    /**
     * Checks if a given file should be skipped when logging.
     *
     * @param filePath The path of the file.
     * @return True if the file should be skipped, false otherwise.
     */
    public boolean shouldSkip(@NotNull String filePath) {
        File file = new File(filePath);

        // Obtain the file extension
        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i + 1);
        }

        // Ignore everything inside this directories
        if (filePath.contains(File.separator + ".idea" + File.separator) ||
                filePath.contains(File.separator + "cmake-build-debug" + File.separator) ||
                filePath.contains(File.separator + "Testing" + File.separator) ||
                filePath.contains(File.separator + "CMakeFiles" + File.separator) ||
                filePath.contains(File.separator + ".cmake" + File.separator)) {
            return true;
        }

        return file.getName().equals(".uoc.data") ||
                file.getName().equals("catalog.json") ||
                file.getName().equals("a.dummy") ||
                file.getName().startsWith("index-20") ||
                file.getName().endsWith(".uoc") ||
                file.getName().endsWith(".ninja") ||
                i == -1 ||
                file.getName().endsWith("pycharm-debug.egg") || // PyCharm
                file.getName().endsWith("CMakeLists.txt") || // CLion
                file.getName().endsWith(".eslintrc") || // WebStorm
                file.getName().endsWith(".babelrc") ||
                file.getName().endsWith("package.json") ||
                file.getName().endsWith("webpack.config.js") ||
                !relevantExtensions.contains(extension);// Return false if it is not an allowed extension
    }

    public boolean shouldSkipCode(String code) {
        // Return true if code is a string of only whitespace
        if (code.trim().isEmpty()) {
            return true;
        }
        // Return false if code has more than cancelRelevantSequences characters
        if (code.length() > PropertiesLoader.getIntProperty("cancelRelevantSequences")) {
            return false;
        }
        // Check if code begins with any of the sequences in relevantSequences
        for (String sequence : relevantSequences) {
            if (code.startsWith(sequence)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the current timestamp as a formatted string.
     *
     * @return A string representing the current timestamp.
     */
    private @NotNull String time() {
        LocalDateTime time = LocalDateTime.now();
        return "     " + time.getHour() + ":" + time.getMinute() +
                "     (" + time.getDayOfMonth() + "-" + time.getMonthValue() + "-" + time.getYear() + ")     ";
    }

    /**
     * Adds an event to the event buffer.
     *
     * @param content The event to be added to the buffer.
     */
    private void addToBuffer(@NotNull String content) {
        eventBuffer.add(content);
    }

    /**
     * Writes all buffered events to the uoc.data file.
     *
     * This method decrypts the file, writes all events from the event buffer to the file,
     * clears the buffer, and then encrypts the file again. If the project is not a UOC
     * project or if the event buffer is empty, this method does nothing.
     *
     * In case of an IOException during file writing, this method logs the error and throws a RuntimeException.
     */
    public void writeBufferedEventsToFile() {
        if (!isUOCProject || eventBuffer.isEmpty()) return;
        cipherTools.decryptFile(baseDir + "/" + dataFile + ".uoc");
        Path path = Paths.get(baseDir + "/" + dataFile);

        try {
            for (String event : eventBuffer) {
                Files.write(path, event.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            }
            eventBuffer.clear();
        } catch (IOException e) {
            LOG.error("Error writing to uoc.data, writeBufferedEventsToFile()", e);
            throw new RuntimeException(e);
        }
        cipherTools.encryptFile(baseDir + "/" + dataFile);
    }

    /**
     * Stops all currently running tasks managed by the scheduler, if any.
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown(); // Cancels existing tasks
            try {
                // Waits a while for existing tasks to terminate
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();

                    // Waits a while for tasks to respond to being cancelled
                    if (!scheduler.awaitTermination(60, TimeUnit.SECONDS))
                        LOG.warn("The thread pool did not terminate correctly");
                }
            } catch (InterruptedException ie) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}