package com.github.t1;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/fake")
public class FakeService {

    @GET
    @Produces(TEXT_PLAIN)
    public String fake() {return "FakeService is working!";}
}
