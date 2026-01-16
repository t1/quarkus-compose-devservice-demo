package com.github.t1;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.QueueConfiguration;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

import static software.amazon.awssdk.services.s3.model.Event.S3_OBJECT_CREATED;

@ApplicationScoped
@Accessors(fluent = true)
@Slf4j
@SuppressWarnings("resource")
public class S3 implements AutoCloseable {
    private final URI endpoint;
    private final String region;
    private final String username;
    private final String password;

    @Getter(lazy = true)
    private final S3Client s3 = S3Client.builder()
            .endpointOverride(endpoint)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(username, password)))
            .region(Region.of(region))
            .httpClientBuilder(ApacheHttpClient.builder())
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();

    public S3(
            @ConfigProperty(name = "s3.uri") URI endpoint,
            @ConfigProperty(name = "s3.region", defaultValue = "dummy-region") String region,
            @ConfigProperty(name = "s3.username") String username,
            @ConfigProperty(name = "s3.password") String password) {
        this.endpoint = endpoint;
        this.region = region;
        this.username = username;
        this.password = password;
        log.info("using S3 endpoint: {}", this.endpoint);
    }

    public void createBucket(String bucketName) {
        try {
            s3().createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (S3Exception e) {
            var code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
            if ("BucketAlreadyOwnedByYou".equals(code)) {
                throw new BucketAlreadyOwnedByYouException(bucketName, e);
            } else {
                throw e;
            }
        }
    }

    public boolean bucketExists(String bucketName) {
        try {
            s3().headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw new RuntimeException("can't check if bucket exists", e);
        }
    }

    public void setBucketPolicy(String bucketName, String policy) {
        s3().putBucketPolicy(PutBucketPolicyRequest.builder().bucket(bucketName).policy(policy).build());
    }

    public String getBucketPolicy(String bucketName) {
        var resp = s3().getBucketPolicy(GetBucketPolicyRequest.builder().bucket(bucketName).build());
        return resp.policy();
    }

    public void putTextObject(String bucketName, String objectName, String content) {
        s3().putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .contentType("text/plain")
                        .build(),
                RequestBody.fromString(content));
    }

    public String getTextObject(String bucketName, String objectName) {
        return s3().getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(objectName).build()).asUtf8String();
    }

    public void addEventListener(String bucketName, String queue) {
        s3().putBucketNotificationConfiguration(PutBucketNotificationConfigurationRequest.builder()
                .bucket(bucketName)
                .notificationConfiguration(NotificationConfiguration.builder()
                        .queueConfigurations(QueueConfiguration.builder()
                                .queueArn(queue)
                                .events(S3_OBJECT_CREATED)
                                .build())
                        .build())
                .build());
    }

    public void removeObject(String bucketName, String objectName) {
        s3().deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectName).build());
    }

    public void removeBucket(String bucketName) {
        s3().deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }

    @Override public void close() {s3().close();}

    public static class BucketAlreadyOwnedByYouException extends RuntimeException {
        public BucketAlreadyOwnedByYouException(String bucketName, Throwable cause) {
            super(String.format("bucket '%s' already exists and is owned by you", bucketName), cause);
        }
    }
}
