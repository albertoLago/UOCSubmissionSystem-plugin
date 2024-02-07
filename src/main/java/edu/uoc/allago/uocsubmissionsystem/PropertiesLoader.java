package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A utility class for loading properties from a config.properties file.
 * <p>
 * This class reads the properties file when it is loaded.
 * Once loaded, properties can be accessed using the getProperty and getIntProperty methods.
 * <p>
 * Note: This class logs errors using the IntelliJ Logger utility. If an error occurs during the loading of the
 * properties file, it will be logged, and the exception's stack trace will be printed to standard error output.
 */
public class PropertiesLoader {

    private static final Logger LOG = Logger.getInstance(PropertiesLoader.class);

    private static final Properties prop;

    static {
        prop = new Properties();
        try (InputStream input =
                     PropertiesLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            prop.load(input);
        } catch (IOException ex) {
            LOG.error("Error loading config.properties", ex);
            ex.printStackTrace();
        }
    }

    /**
     * Retrieves a property value from the loaded properties file.
     *
     * @param key the key of the property to retrieve.
     * @return the value of the specified property, or null if the property is not found.
     */
    public static String getProperty(String key) {
        return prop.getProperty(key);
    }

    /**
     * Retrieves a property value from the loaded properties file and converts it to an integer.
     *
     * @param key the key of the property to retrieve.
     * @return the integer value of the specified property.
     * @throws NumberFormatException if the property is not a valid integer value.
     */
    public static int getIntProperty(String key) {
        return Integer.parseInt(prop.getProperty(key));
    }
}
