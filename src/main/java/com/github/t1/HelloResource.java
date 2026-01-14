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
    @RegisterRestClient(configKey = "fake-service")
    @Path("/")
    public interface NestedApi {
        @GET @Produces(TEXT_PLAIN) String fake();
    }

    @Inject @RestClient NestedApi nestedApi;


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

    @GET @Path("/fake")
    @Produces(TEXT_PLAIN)
    public String fake() {return "Hello, " + nestedApi.fake();}

    @GET @Path("/s3")
    @Produces(TEXT_PLAIN)
    public String s3() {return "Hello, " + s3.get("test", "s3.txt");}
}
