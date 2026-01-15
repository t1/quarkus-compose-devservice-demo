package com.github.t1;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static com.github.t1.S3Test.ENDPOINT;
import static io.restassured.RestAssured.given;
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
                .when().get(ENDPOINT + "/test/s3.txt")
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
}
