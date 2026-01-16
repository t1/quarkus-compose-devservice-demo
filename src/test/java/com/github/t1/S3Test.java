package com.github.t1;

import com.github.t1.S3.BucketAlreadyOwnedByYouException;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.util.Locale.ROOT;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.CoreMatchers.is;

@Slf4j
@QuarkusTest
class S3Test {
    /// has to find the (random) port of the S3; see README for details
    public static String s3uri() {
        return ConfigProvider.getConfig().getValue("s3.uri", String.class);
    }

    // we can't @Inject this, as we want it to be static, so the nested @BeforeAll can use it, too
    @SuppressWarnings("resource")
    private static final S3 S3 = Arc.container().instance(S3.class).get();

    private static JsonValue json(String string) {
        return Json.createReader(new StringReader(string)).readValue();
    }

    @Nested class WithBucket {
        static final String bucketName = "test-" + UUID.randomUUID().toString().substring(0, 8).toLowerCase(ROOT);

        @BeforeAll static void shouldCreateBucket() {
            try {
                log.info("create bucket: {}", bucketName);
                S3.createBucket(bucketName);
                log.info("bucket created: {}", bucketName);
            } catch (BucketAlreadyOwnedByYouException e) {
                log.info("bucket already exists: {}", bucketName);
            }
        }

        @Test void shouldExist() {
            log.info("test bucket exists: {}", bucketName);
            then(S3.bucketExists(bucketName)).isTrue();
        }

        @Nested class WithDownloadAccess {
            static final JsonValue DOWNLOAD_POLICY = json("""
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
                          "Resource": ["arn:aws:s3:::<BUCKET_NAME>"]
                        },
                        {
                          "Effect": "Allow",
                          "Principal": {
                            "AWS": ["*"]
                          },
                          "Action": [
                            "s3:GetObject"
                          ],
                          "Resource": ["arn:aws:s3:::<BUCKET_NAME>/*"]
                        }
                      ]
                    }""".replace("<BUCKET_NAME>", bucketName));

            @BeforeAll static void shouldSetDownloadAccess() {
                log.info("set policy for {}", bucketName);
                S3.setBucketPolicy(bucketName, DOWNLOAD_POLICY.toString());
                log.info("download access set for bucket {}", bucketName);
            }

            @Test void shouldHaveDownloadAccess() {
                log.info("get policy for {}", bucketName);

                var actual = json(S3.getBucketPolicy(bucketName)
                        // the order of actions is not guaranteed
                        .replace("\"Action\":[\"s3:ListBucket\",\"s3:GetBucketLocation\"]",
                                "\"Action\":[\"s3:GetBucketLocation\",\"s3:ListBucket\"]"));

                then(actual).isEqualTo(DOWNLOAD_POLICY);
            }

            @Nested class WithFile {
                static final String objectName = "s3.txt";
                static final String content = "S3 file available";

                @BeforeAll static void shouldUploadFile() {
                    log.info("put file: {}/{}", bucketName, objectName);
                    S3.putTextObject(bucketName, objectName, content);
                }

                @Test void shouldGetFile() {
                    log.info("get file: {}/{}", bucketName, objectName);

                    var response = S3.getTextObject(bucketName, objectName);

                    then(response).isEqualTo(content);
                }

                @Test void shouldAccessFileWithHttp() {
                    var path = s3uri() + "/" + bucketName + "/" + objectName;
                    log.info("access file via {}", path);
                    given()
                            .when().get(path)
                            .then()
                            .statusCode(200)
                            .body(is(content));
                }

                @AfterAll static void shouldRemoveFile() {
                    log.info("remove file: {}/{}", bucketName, objectName);
                    S3.removeObject(bucketName, objectName);
                    log.info("file removed: {}/{}", bucketName, objectName);
                }
            }
        }

        @AfterAll static void shouldRemoveBucket() {
            log.info("remove bucket {}", bucketName);
            S3.removeBucket(bucketName);
            log.info("bucket removed {}", bucketName);

            then(S3.bucketExists(bucketName)).isFalse();
        }
    }
}
