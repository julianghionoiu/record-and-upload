package tdl.record_upload.credentials;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Condition;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetFederationTokenRequest;

import java.util.Arrays;

public class FederatedCredentialsProvider {

    private final String s3Bucket;
    private final AWSSecurityTokenService tokenService;

    public FederatedCredentialsProvider(String accessKey, String secretKey, String region, String bucket) {
        s3Bucket = bucket;

        tokenService = AWSSecurityTokenServiceClientBuilder
                .standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .build();
    }

    public Credentials getCredentials(String userNAme) {
        GetFederationTokenRequest getFederationTokenRequest = new GetFederationTokenRequest()
                .withName(userNAme)
                .withPolicy(getPolicy(userNAme).toJson());
        return tokenService.getFederationToken(getFederationTokenRequest).getCredentials();
    }

    private Policy getPolicy(String userName) {
        Statement multipartUploadStatement = getMultipartUploadStatement(userName);
        Statement creatingObjectsStatement = getObjectCreatingStatement(userName);

        return new Policy("Files uploading policy", Arrays.asList(multipartUploadStatement, creatingObjectsStatement));
    }

    private Statement getObjectCreatingStatement(String userName) {
        return new Statement(Statement.Effect.Allow)
                .withActions(
                        () -> "s3:PutObject",
                        () -> "s3:GetObject"
                )
                .withResources(new Resource("arn:aws:s3:::" + s3Bucket + "/" + userName + "/*"));
    }

    private Statement getMultipartUploadStatement(String userName) {
        return new Statement(Statement.Effect.Allow)
                .withActions(
                        () -> "s3:ListBucket",
                        () -> "s3:ListBucketMultipartUploads"
                )
                .withResources(new Resource("arn:aws:s3:::" + s3Bucket))
                .withConditions(
                        new Condition()
                                .withType("StringEquals")
                                .withConditionKey("s3:prefix")
                                .withValues(userName)
                );
    }
}
