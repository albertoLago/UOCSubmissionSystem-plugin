package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name
 * of the data and the file name where these persistent application settings are stored.
 * Read more:
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html#creating-the-appsettingstate-implementation
 */

/**
 * Represents the state of the application settings.
 */
@State(
        name = "edu.uoc.allago.uocsubmissionsystem.DataPersistence",
        storages = {@Storage("uocsubmissionsystem.xml")}
)
public class AppSettingsState implements PersistentStateComponent<AppSettingsState> {

    // Stored variables
    public String fullName = "";
    public String userId = "";
    public String server = "";
    public String poolID = "";
    public List<String> projects = new ArrayList<>();

    /**
     * Gets the instance of the AppSettingsState class.
     * @return The instance of the AppSettingsState class.
     */
    public static AppSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(AppSettingsState.class);
    }

    /**
     * Gets the state of the application settings.
     * @return The state of the application settings.
     */
    @Nullable
    @Override
    public AppSettingsState getState() {
        return this;
    }

    /**
     * Loads the state of the application settings.
     * @param state The state of the application settings to load.
     */
    @Override
    public void loadState(@NotNull AppSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * Creates a new set of credential attributes for storing user passwords.
     * @return A new set of credential attributes for storing user passwords.
     */
    private CredentialAttributes createCredentialAttributes() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(
                        "uocsubmissionsystem-plugin", "uocsubmissionsystem-plugin-key")
        );
    }

    /**
     * Creates a new set of credential attributes for storing admin passwords.
     * @return A new set of credential attributes for storing admin passwords.
     */
    private CredentialAttributes createCredentialAttributesMaster() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(
                        "uocsubmissionsystem-plugin-master", "uocsubmissionsystem-plugin-key-master")
        );
    }

    /**
     * Saves the user's password to the password store.
     * @param userId The ID of the user.
     * @param password The user's password.
     * @param admin Whether the user is an admin or not.
     */
    public void savePassword(String userId, String password, boolean admin) {
        CredentialAttributes credentialAttributes;
        if(admin)  credentialAttributes = createCredentialAttributesMaster();
        else credentialAttributes = createCredentialAttributes();

        if (password != null) {
            Credentials credentials = new Credentials(userId, password);
            PasswordSafe.getInstance().set(credentialAttributes, credentials);
        } else {
            // clear the store
            PasswordSafe.getInstance().set(credentialAttributes, null);
        }
    }

    /**
     * Retrieves the user's password from the password store.
     * @param admin Whether the user is an admin or not.
     * @return The user's password.
     */
    public String retrievePassword(boolean admin) {
        CredentialAttributes credentialAttributes;
        if(admin)  credentialAttributes = createCredentialAttributesMaster();
        else credentialAttributes = createCredentialAttributes();
        String password;

        Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
        if (credentials != null) {
            password = credentials.getPasswordAsString();
        } else {
            // password only
            password = PasswordSafe.getInstance().getPassword(credentialAttributes);
        }
        return password;
    }
}
