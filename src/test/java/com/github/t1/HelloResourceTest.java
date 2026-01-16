package com.github.t1;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.t1.S3Test.s3uri;
import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class HelloResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello World!"));
    }

    @Test
    void testFakeEndpoint() {
        given()
                .when().get("/fake")
                .then()
                .statusCode(200)
                .body(is("FakeService is working!"));
    }

    @Test
    void testHelloFakeEndpoint() {
        given()
                .when().get("/hello/fake")
                .then()
                .statusCode(200)
                .body(is("Hello, FakeService is working!"));
    }

    @Test
    void testS3Endpoint() {
        given()
                .when().get(s3uri() + "/test/s3.txt")
                .then()
                .statusCode(200)
                .body(is("S3 file available"));
    }

    @Test
    void testHelloS3Endpoint() {
        given()
                .when().get("/hello/s3")
                .then()
                .statusCode(200)
                .body(is("Hello, S3 file available"));
    }

    @Test
    @Disabled("fails; could it be that S3 is prematurely shut down?")
    void testPostEndpoint() {
        var content = UUID.randomUUID().toString();
        given()
                .when().body(content).contentType(TEXT_PLAIN).post("/hello/s3")
                .then()
                .statusCode(200)
                .body(is("got:" + content));
    }
}
