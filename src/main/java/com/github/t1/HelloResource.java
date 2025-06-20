package com.github.t1;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/hello")
public class HelloResource {
    @RegisterRestClient(configKey = "mock-endpoint")
    @Path("/")
    public interface MockApi {
        @GET @Produces(TEXT_PLAIN) String mock();
    }

    @Inject @RestClient MockApi mockApi;

    @GET
    @Produces(TEXT_PLAIN)
    public String hello() {return "Hello World!";}

    @GET @Path("/mock")
    @Produces(TEXT_PLAIN)
    public String mock() {return "Hello, " + mockApi.mock();}
}
