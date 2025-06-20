package com.github.t1;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Startup
@ApplicationScoped
@Slf4j
public class TestDataLoader {
    private static final String bucketName = "test";
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
                }
              ]
            }""";

    @Inject S3 s3;

    @PostConstruct
    void init() {
        log.info("TestDataLoader initialized. You can load your test data here.");
        createBucket();
        putFile();
    }

    private void createBucket() {
        log.info("make bucket: {}", bucketName);
        s3.makeBucket(bucketName);

        log.info("set access policy to DOWNLOAD for bucket {}", bucketName);
        s3.setBucketPolicy(bucketName, DOWNLOAD_POLICY);

        log.info("bucket made: {}", bucketName);
    }

    private void putFile() {
        s3.putTextObject(bucketName, objectName, content);
        log.info("file uploaded: {}/{}", bucketName, objectName);
    }
}
