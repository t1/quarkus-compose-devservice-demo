package com.github.t1;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/hello")
public class HelloResource {
    @RegisterRestClient(configKey = "mock-service")
    @Path("/")
    public interface MockApi {
        @GET @Produces(TEXT_PLAIN) String mock();
    }

    @Inject @RestClient MockApi mockApi;


    /// We could use the S3 class here, but using a REST client is so much simpler.
    // TODO instead of setting the bucket policy to `download`, we could use a presigned URL.
    @RegisterRestClient(configKey = "s3")
    @Path("/{bucketName}/{objectPath: .*}")
    public interface S3Api {
        @GET @Produces(TEXT_PLAIN) String get(
                @PathParam("bucketName") String bucketName,
                @PathParam("objectPath") String objectPath);
    }

    @Inject @RestClient S3Api s3;

    @GET
    @Produces(TEXT_PLAIN)
    public String hello() {return "Hello World!";}

    @GET @Path("/mock")
    @Produces(TEXT_PLAIN)
    public String mock() {return "Hello, " + mockApi.mock();}

    @GET @Path("/s3")
    @Produces(TEXT_PLAIN)
    public String s3() {return "Hello, " + s3.get("test", "s3.txt");}
}
