package com.github.t1;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import static com.github.t1.HelloResource.BUCKET_NAME;
import static com.github.t1.S3.BucketAlreadyOwnedByYouException;

@Startup
@ApplicationScoped
@Slf4j
public class TestDataLoader {
    private static final String objectName = "s3.txt";
    private static final String content = "S3 file available";
    private static final String DOWNLOAD_POLICY = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {
                    "AWS": ["*"]
                  },
                  "Action": [
                    "s3:GetBucketLocation",
                    "s3:ListBucket"
                  ],
                  "Resource": ["arn:aws:s3:::test"]
                },
                {
                  "Effect": "Allow",
                  "Principal": {
                    "AWS": ["*"]
                  },
                  "Action": [
                    "s3:GetObject"
                  ],
                  "Resource": ["arn:aws:s3:::test/*"]
                },
                {
                  "Effect": "Allow",
                  "Principal": {
                    "AWS": ["*"]
                  },
                  "Action": [
                    "s3:PutObject"
                  ],
                  "Resource": ["arn:aws:s3:::test/*"]
                }
              ]
            }""";

    @Inject S3 s3;

    @PostConstruct
    void init() {
        log.info("initializing test data in S3 bucket: {}/{}", BUCKET_NAME, objectName);
        createBucket();
        putFile();
        log.info("initialized test data in S3 bucket: {}/{}", BUCKET_NAME, objectName);
    }

    private void createBucket() {
        log.info("create bucket: {}", BUCKET_NAME);

        try {
            s3.createBucket(BUCKET_NAME);
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("bucket already exists: {}", BUCKET_NAME);
        }

        log.info("set access policy to DOWNLOAD for bucket {}", BUCKET_NAME);
        s3.setBucketPolicy(BUCKET_NAME, DOWNLOAD_POLICY);

        log.info("bucket created: {}", BUCKET_NAME);
    }

    private void putFile() {
        s3.putTextObject(BUCKET_NAME, objectName, content);
        log.info("file uploaded: {}/{}", BUCKET_NAME, objectName);
    }
}
