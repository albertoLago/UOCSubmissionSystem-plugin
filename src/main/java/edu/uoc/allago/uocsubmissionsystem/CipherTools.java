package edu.uoc.allago.uocsubmissionsystem;

import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.spec.KeySpec;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Provides functionality for encrypting and decrypting files using AES encryption.
 */
public class CipherTools {

    private final String password;

    /**
     * Constructor that initializes the password from the config.properties file.
     */
    public CipherTools() {
        // Retrieve the password from the properties
        password = PropertiesLoader.getProperty("key");
    }

    /**
     * Encrypts all eligible files in a project directory.
     *
     * @param basePath the base directory of the project
     */
    public void encryptProject(String basePath) {
        Path path = Paths.get(basePath);

        try (Stream<Path> paths = Files.walk(path)) {
            paths.filter(p -> {
                        // Exclude ".idea" directory
                        boolean isIdeaDir = p.startsWith(path.resolve(".idea"));
                        // Exclude ".iml" files
                        boolean isImlFile = p.getFileName().toString().endsWith(".iml");
                        // Exclude ".uoc" files
                        boolean isUocFile = p.getFileName().toString().endsWith(".uoc");
                        // Exclude "CMakeLists.txt" files
                        boolean isCMakeLists = p.getFileName().toString().endsWith("CMakeLists.txt");
                        // Exclude "venv" directory
                        boolean isVenvDir = p.startsWith(path.resolve("venv"));

                        return Files.isRegularFile(p) && !isIdeaDir && !isImlFile &&
                                !isUocFile && !isVenvDir && !isCMakeLists;
                    })
                    .forEach(file -> encrypt(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Decrypts all eligible ".uoc" files in a project directory, except "uoc.data".
     *
     * @param basePath the base directory of the project
     * @return the number of decrypted files
     */
    public int decryptProject(String basePath) {
        Path path = Paths.get(basePath);
        AtomicInteger count = new AtomicInteger();

        try (Stream<Path> paths = Files.walk(path)) {
            paths.filter(p -> {
                        // Only include ".uoc" files
                        boolean isUocFile = p.getFileName().toString().endsWith(".uoc");
                        // Exclude "uoc.data.uoc" files
                        boolean isUOCdataFile = p.getFileName().toString().endsWith("uoc.data.uoc");

                        return Files.isRegularFile(p) && isUocFile && !isUOCdataFile;
                    })
                    .forEach(file -> {
                        decrypt(file);
                        count.getAndIncrement();
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count.get();
    }

    /**
     * Encrypts a single file.
     *
     * @param file the path to the file to encrypt
     */
    public void encryptFile(String file) {
        Path path = Paths.get(file);
        encrypt(path);
        File f = new File(file + ".uoc");
        try {
            setHiddenProperty(f);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts a single file.
     *
     * @param file the path to the file to decrypt
     */
    public void decryptFile(String file) {
        Path path = Paths.get(file);
        decrypt(path);
    }

    private void cipherFile(SecretKey key, int cipherMode, File inputFile, File outputFile) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(cipherMode, key);

        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[64];
            int bytesRead;

            // Read input file and update the cipher with its content
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] outputBuffer = cipher.update(buffer, 0, bytesRead);
                if (outputBuffer != null) {
                    outputStream.write(outputBuffer);
                }
            }
            // Finalize the encryption or decryption process
            byte[] outputBytes = cipher.doFinal();
            if (outputBytes != null) {
                outputStream.write(outputBytes);
            }
        }
    }

    // Generate a secret key for encryption or decryption using a password
    private @NotNull SecretKey generateKey(@NotNull String password) throws Exception {
        // Use a fixed salt for key derivation
        byte[] salt = "s0m3s@l7".getBytes();

        // Set the number of iterations and key length
        int iterations = 500;
        int keyLength = 128;

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        byte[] keyBytes = secretKeyFactory.generateSecret(keySpec).getEncoded();

        return new SecretKeySpec(keyBytes, "AES");
    }

    // Encrypt a file at the given path
    private void encrypt(@NotNull Path path) {
        File inputFile = new File(path.toString());
        File encryptedFile = new File(path.toString() + ".uoc");

        try {
            // Generate the secret key
            SecretKey secretKey = generateKey(this.password);

            // Encrypt the file
            cipherFile(secretKey, Cipher.ENCRYPT_MODE, inputFile, encryptedFile);
            if (inputFile.exists()) inputFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Decrypt a file at the given path
    private void decrypt(@NotNull Path path) {
        File encryptedFile = new File(path.toString());
        String p = path.toString();
        File decryptedFile = new File(p.substring(0, p.length() - 4));

        try {
            // Generate a secret key
            SecretKey secretKey = generateKey(password);

            // Decrypt the file
            if (encryptedFile.exists()) {
                cipherFile(secretKey, Cipher.DECRYPT_MODE, encryptedFile, decryptedFile);
                encryptedFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setHiddenProperty(File file) throws InterruptedException, IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            ProcessBuilder pb = new ProcessBuilder("attrib", "+H", file.getAbsolutePath());
            Process p = pb.start();
            p.waitFor();
        }
    }
}