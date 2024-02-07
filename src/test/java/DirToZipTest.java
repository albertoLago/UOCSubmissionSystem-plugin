import edu.uoc.allago.uocsubmissionsystem.DirToZip;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class DirToZipTest {

    private DirToZip dirToZip;
    private Path testFile;
    private Path copiedFile;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        dirToZip = new DirToZip();
    }

    @After
    public void tearDown() throws IOException {
        if(testFile != null && Files.exists(testFile)) {
            Files.delete(testFile);
        }
        if(copiedFile != null && Files.exists(copiedFile)) {
            Files.delete(copiedFile);
        }
    }

    @Test
    public void testZip() throws IOException {
        File tempDir = tempFolder.newFolder("testDir");
        testFile = new File(tempDir, "test.txt1").toPath();
        Files.write(testFile, "This is a test file.".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dirToZip.zip(tempDir.getAbsolutePath(), outputStream);

        assertFalse(outputStream.size() == 0);
    }

    @Test
    public void testCopyToTemp() throws IOException {
        Path tempDir = Paths.get("src/test/resources/tempDirectory");

        testFile = tempDir.resolve("test1.txt");
        Files.createDirectories(tempDir);
        Files.write(testFile, "This is a test file.".getBytes(StandardCharsets.UTF_8));

        //dirToZip.excludeStandard();
        dirToZip.copyToTemp(tempDir.toString());

        Path destinationPath = Paths.get(System.getProperty("java.io.tmpdir") + "/uoctemp");
        copiedFile = destinationPath.resolve("test1.txt");
        assertTrue(Files.exists(copiedFile));

        String copiedContent = new String(Files.readAllBytes(copiedFile), StandardCharsets.UTF_8);
        assertEquals("This is a test file.", copiedContent);
    }

    @Test(expected = IOException.class)
    public void testZipThrowsException() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dirToZip.zip("non/existent/path", outputStream);
    }

    @Test(expected = IOException.class)
    public void testCopyToTempThrowsException() throws IOException {
        dirToZip.copyToTemp("non/existent/path");
    }
}
