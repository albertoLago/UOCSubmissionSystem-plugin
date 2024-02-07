package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.application.ApplicationInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class allows compressing directories into .zip files
 * excluding files and folders based on specified patterns.
 */
public class DirToZip {

    // Files that will be excluded from the .zip file
    private final ArrayList<Pattern> excluded = new ArrayList<>();

    private void exclude(String regex) {
        excluded.add(Pattern.compile(regex));
    }

    /**
     * Adds standard excluded patterns.
     */
    public void excludeStandard() {

        exclude("[\\\\/](html|latex|rtf)[\\\\/]");
        exclude("[\\\\/]cmake-");
        exclude("(\\.zip$)|(\\.zip[\\\\/])");
        exclude("\\.(o|O)$");
        //exclude("[\\\\/]venv($|[\\\\/])");  // PyCharm
        exclude("\\.iml$");
        //exclude("[\\\\/]__pycache__[\\\\/]");  // PyCharm
        //exclude("[\\\\/]dist[\\\\/]");  // PyCharm
        //exclude("[\\\\/]venv[\\\\/]");  // PyCharm
        //exclude("[\\\\/]out[\\\\/]");  // IntelliJ
        //exclude("[\\\\/]target[\\\\/]");  // IntelliJ
        //exclude("[\\\\/]build[\\\\/]");  // CLion
        //exclude("[\\\\/]node_modules[\\\\/]");  // WebStorm

        if (!isAndroidStudio()) {
            exclude("[\\\\^/]\\.");
        }
    }

    private boolean toExclude(String path) {
        Pattern ALLOWED_FILE = Pattern.compile("\\.uoc\\.data\\.uoc$");
        Matcher matcher = ALLOWED_FILE.matcher(path);
        if (matcher.find()) {
            // Do not exclude if it is the allowed file
            return false;
        }

        for (Pattern pattern : excluded) {
            matcher = pattern.matcher(path);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    private boolean isAndroidStudio() {
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
            String versionName = applicationInfo.getVersionName();
            return versionName.contains("Android Studio");
    }


    /**
     * Compresses the specified directory into a .zip file.
     *
     * @param sourceDirPath The path of the directory to be compressed.
     * @param stream        The OutputStream to write the .zip file to.
     * @throws IOException If there is an issue creating the .zip file.
     */
    public void zip(String sourceDirPath, OutputStream stream) throws IOException {
        try (ZipOutputStream zs = new ZipOutputStream(stream)) {
            Path pp = Paths.get(sourceDirPath);
            Files.walk(pp)
                    .forEach(path -> {
                        if (!toExclude(path.toString())) {
                            String relativePath = pp.relativize(path).toString();
                            if (File.separatorChar != '/') {
                                relativePath = relativePath.replace('\\', '/');
                            }
                            ZipEntry zipEntry = new ZipEntry(relativePath + (Files.isDirectory(path) ? "/" : ""));
                            try {
                                zs.putNextEntry(zipEntry);
                                if (!Files.isDirectory(path)) {
                                    Files.copy(path, zs);
                                }
                                zs.closeEntry();
                            } catch (IOException e) {
                                System.err.println(e.getLocalizedMessage());
                            }
                        }
                    });
            zs.flush();
        }
    }

    /**
     * Copies the specified directory to a temporary directory,
     * excluding files and folders based on the exclusion patterns.
     *
     * @param sourceDirPath The path of the directory to be copied.
     * @throws IOException If there is an issue copying the directory.
     */
    public void copyToTemp(String sourceDirPath) throws IOException {
        Path sourcePath = Paths.get(sourceDirPath);
        Path destinationPath = Paths.get(System.getProperty("java.io.tmpdir") + "/uoctemp");

        // Ensure temp directory exists
        if (!Files.exists(destinationPath)) {
            Files.createDirectories(destinationPath);
        }

        Files.walk(sourcePath)
                .filter(path -> !path.equals(destinationPath)) // Excluir la carpeta temporal
                .forEach(path -> {
                    if (!toExclude(path.toString()) && !isSubPath(destinationPath, path)) {
                        Path targetPath = destinationPath.resolve(sourcePath.relativize(path));
                        try {
                            if (Files.isDirectory(path)) {
                                if (!Files.exists(targetPath)) {
                                    Files.createDirectories(targetPath);
                                }
                            } else {
                                Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            System.err.println(e.getLocalizedMessage());
                        }
                    }
                });
    }

    private boolean isSubPath(Path basePath, @NotNull Path checkPath) {
        Path parentPath = checkPath.getParent();
        while (parentPath != null) {
            if (parentPath.equals(basePath)) {
                return true;
            }
            parentPath = parentPath.getParent();
        }
        return false;
    }
}
