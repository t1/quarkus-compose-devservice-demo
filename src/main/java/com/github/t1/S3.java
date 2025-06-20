package com.github.t1;

import io.minio.BucketExistsArgs;
import io.minio.GetBucketPolicyArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static io.minio.ObjectWriteArgs.MIN_MULTIPART_SIZE;

@ApplicationScoped
@Accessors(fluent = true)
@SuppressWarnings("resource")
public
class S3 implements AutoCloseable {
    private final String endpoint;
    private final String username;
    private final String password;

    @Getter(lazy = true)
    private final MinioClient s3 = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(username, password)
            .build();

    public S3(
            @ConfigProperty(name = "quarkus.rest-client.s3.uri") String endpoint,
            @ConfigProperty(name = "quarkus.rest-client.s3.username") String username,
            @ConfigProperty(name = "quarkus.rest-client.s3.password") String password) {
        this.endpoint = endpoint;
        this.username = username;
        this.password = password;
    }

    public void makeBucket(String bucketName) {
        try {
            s3().makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (ErrorResponseException e) {
            if ("BucketAlreadyOwnedByYou".equals(e.errorResponse().code())) {
                throw new BucketAlreadyOwnedByYouException(bucketName, e);
            } else {
                throw new RuntimeException("can't make bucket " + bucketName, e);
            }
        } catch (Exception e) {
            throw new RuntimeException("can't make bucket " + bucketName, e);
        }
    }

    public boolean bucketExists(String bucketName) {
        try {
            return s3().bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException("can't check if bucket exists", e);
        }
    }

    public void setBucketPolicy(String bucketName, String policy) {
        try {
            s3().setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getBucketPolicy(String bucketName) {
        try {
            return s3().getBucketPolicy(GetBucketPolicyArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException("can't get bucket policy", e);
        }
    }

    public ObjectWriteResponse putTextObject(String bucketName, String objectName, String content) {
        try {
            return s3().putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .contentType("text/plain")
                    .stream(new ByteArrayInputStream(content.getBytes()), -1, MIN_MULTIPART_SIZE)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("can't put text object", e);
        }
    }

    public String getTextObject(String bucketName, String objectName) {
        var response = getObject(bucketName, objectName);
        try {
            return new String(response.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("can't read content of text object", e);
        }
    }

    public GetObjectResponse getObject(String bucketName, String objectName) {
        try {
            return s3().getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("can't get object", e);
        }
    }

    public void removeObject(String bucketName, String objectName) {
        try {
            s3().removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void removeBucket(String bucketName) {
        try {
            s3().removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void close() {
        try {
            s3().close();
        } catch (Exception e) {
            throw new RuntimeException("can't close", e);
        }
    }

    public static class BucketAlreadyOwnedByYouException extends RuntimeException {
        public BucketAlreadyOwnedByYouException(String bucketName, Throwable cause) {
            super(String.format("bucket '%s' already exists and is owned by you", bucketName), cause);
        }
    }
}
