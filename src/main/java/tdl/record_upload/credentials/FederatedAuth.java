package tdl.record_upload.credentials;

import com.amazonaws.services.securitytoken.model.Credentials;

import java.io.*;
import java.util.Properties;

public class FederatedAuth {

    private final FederatedCredentialsProvider credentialsProvider;
    private final String bucket;
    private final String user;

    public FederatedAuth(String propertiesFileName) throws IOException {
        try (Reader reader = new FileReader(propertiesFileName)) {
            Properties properties = new Properties();
            properties.load(reader);
            String awsAccessKeyId = properties.getProperty("aws_access_key_id");
            String awsSecretAccessKey = properties.getProperty("aws_secret_access_key");

            String s3Region = properties.getProperty("s3_region");
            bucket = properties.getProperty("s3_bucket");
            user = properties.getProperty("s3_prefix");
            credentialsProvider = new FederatedCredentialsProvider(awsAccessKeyId, awsSecretAccessKey, s3Region, bucket);
        }
    }

    public Properties getTempUserCredentials() {
        Credentials credentials = credentialsProvider.getCredentials(user);
        return new Properties(){{
            put("aws_access_key_id", credentials.getAccessKeyId());
            put("aws_secret_access_key", credentials.getSecretAccessKey());
            put("aws_session_token", credentials.getSessionToken());
            put("s3_region", "us-west-2");
            put("s3_bucket", bucket);
            put("s3_prefix", user + "/");
        }};
    }

    public void saveTempCredentials(String fileName) throws IOException {
        try (Writer writer = new FileWriter(fileName)) {
            getTempUserCredentials().store(writer, "");
        }
    }

}
