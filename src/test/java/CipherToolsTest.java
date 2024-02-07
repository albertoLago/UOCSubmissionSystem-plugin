import edu.uoc.allago.uocsubmissionsystem.CipherTools;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class CipherToolsTest {

    private CipherTools cipherTools;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        cipherTools = new CipherTools();
    }

    @Test
    public void testEncryptDecryptFile() throws IOException {
        // Create a temporary file with some content
        File inputFile = tempFolder.newFile("test.txt");
        Path inputPath = inputFile.toPath();
        Files.write(inputPath, "This is a test file.".getBytes(StandardCharsets.UTF_8));

        // Encrypt the file
        cipherTools.encryptFile(inputPath.toString());

        // Verify that the encrypted file exists and the original file is deleted
        Path encryptedFile = tempFolder.getRoot().toPath().resolve("test.txt.uoc");
        assertTrue(Files.exists(encryptedFile));
        assertFalse(Files.exists(inputPath));

        // Decrypt the file
        cipherTools.decryptFile(encryptedFile.toString());

        // Verify that the decrypted file exists and the encrypted file is deleted
        assertTrue(Files.exists(inputPath));
        assertFalse(Files.exists(encryptedFile));

        // Check the content of the decrypted file
        String decryptedContent = new String(Files.readAllBytes(inputPath), StandardCharsets.UTF_8);
        assertEquals("This is a test file.", decryptedContent);
    }
}
