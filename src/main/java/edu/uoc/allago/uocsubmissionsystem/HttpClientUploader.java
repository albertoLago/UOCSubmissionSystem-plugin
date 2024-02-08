package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;


/**
 * This class provides the functionality to upload zip files to a server using HttpClient.
 */
public class HttpClientUploader {

    private static final Logger LOG = Logger.getInstance(HttpClientUploader.class);

    /**
     * This method uploads a zip file to a server.
     *
     * @param zipFile  The file to be uploaded.
     * @param fileName The name of the file to be uploaded.
     * @return Returns 1 if the upload is successful, 2 if the server is not online, and 0 if an error occurred during upload.
     */
    public static int uploadZipFile(File zipFile, String fileName) {
        AppSettingsState appSettingsState = AppSettingsState.getInstance();

        HttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(appSettingsState.server + "/upload/" + appSettingsState.poolID);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(
                "file",
                zipFile,
                ContentType.create("application/zip"),
                fileName + ".zip"
        );
        httpPost.setEntity(builder.build());

        if(!isServerOnline(appSettingsState.server)) {
            return 2;
        }

        try {
            String response = httpClient.execute(httpPost, httpResponse -> EntityUtils.toString(httpResponse.getEntity()));
            if (response.equals("success")) {
                LOG.info("Project successfully submitted.");
                return 1;
            } else {
                LOG.warn("Error submitting the project. Server response: " + response);
                return 0;
            }
        } catch (IOException e) {
            LOG.warn("Error submitting the project: " + e.getMessage(), e);
            return 0;
        } catch (Exception e) {
            LOG.error("Unexpected error: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * This method checks if the server is online.
     *
     * @param serverUrl The URL of the server to check.
     * @return Returns true if the server is online, false otherwise.
     */
    private static boolean isServerOnline(String serverUrl) {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(serverUrl);
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            return (statusCode >= 200 && statusCode < 300);
        } catch (IOException e) {
            LOG.error("Failed to check if server is online: " + e.getMessage(), e);
            return false;
        }
    }
}