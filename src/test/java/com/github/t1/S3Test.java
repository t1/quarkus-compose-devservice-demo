package com.github.t1;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.CoreMatchers.is;

@Slf4j
@QuarkusTest
class S3Test {
    private static final Config config = ConfigProvider.getConfig();
    public static final String ENDPOINT = "http://localhost:" + s3config("port");
    static S3 S3 = new S3(ENDPOINT, s3config("username"), s3config("password"));

    private static String s3config(String key) {
        return config.getValue("quarkus.rest-client.s3." + key, String.class);
    }

    static JsonValue json(String string) {
        return Json.createReader(new StringReader(string)).readValue();
    }

    @Nested class WithBucket {
        static final String bucketName = "test";

        @BeforeAll static void shouldMakeBucket() {
            try {
                S3.makeBucket(bucketName);
                log.info("bucket made: {}", bucketName);
            } catch (S3.BucketAlreadyOwnedByYouException e) {
                log.info("bucket already exists: {}", bucketName);
            }
        }

        @Test void shouldExist() {
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
                    }""");

            @BeforeAll static void shouldSetDownloadAccess() {
                S3.setBucketPolicy(bucketName, DOWNLOAD_POLICY.toString());
                log.info("download access set for bucket {}", bucketName);
            }

            @Test void shouldHaveDownloadAccess() {
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
                    var response = S3.putTextObject(bucketName, objectName, content);
                    then(response.bucket()).isEqualTo(bucketName);
                    then(response.object()).isEqualTo(objectName);
                    log.info("file uploaded: {}/{}", bucketName, objectName);
                }

                @Test void shouldGetFile() {
                    var response = S3.getTextObject(bucketName, objectName);

                    then(response).isEqualTo(content);
                }

                @Test void shouldAccessFileWithHttp() {
                    given()
                            .when().get(ENDPOINT + "/" + bucketName + "/" + objectName)
                            .then()
                            .statusCode(200)
                            .body(is(content));
                }

                @AfterAll static void shouldRemoveFile() {
                    S3.removeObject(bucketName, objectName);
                    log.info("file removed: {}/{}", bucketName, objectName);
                }
            }
        }

        @AfterAll static void shouldRemoveBucket() {
            S3.removeBucket(bucketName);
            log.info("bucket removed {}", bucketName);

            then(S3.bucketExists(bucketName)).isFalse();
        }
    }

    @AfterAll static void closeS3() {
        S3.close();
    }
}
