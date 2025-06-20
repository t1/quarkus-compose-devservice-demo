package com.github.t1;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

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
    void testMockEndpoint() {
        given()
                .when().get("/mock")
                .then()
                .statusCode(200)
                .body(is("MockService is working!"));
    }

    @Test
    void testHelloMockEndpoint() {
        given()
                .when().get("/hello/mock")
                .then()
                .statusCode(200)
                .body(is("Hello, MockService is working!"));
    }

    @Test
    void testS3Endpoint() {
        given()
                .when().get("http://localhost:9000/test/s3.txt")
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
