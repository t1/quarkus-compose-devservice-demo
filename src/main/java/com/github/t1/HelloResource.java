package com.github.t1;

import com.github.t1.S3Events.ObjectCreatedEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/hello")
@Slf4j
public class HelloResource {
    public static final String BUCKET_NAME = "test";

    @RegisterRestClient(configKey = "fake-service")
    @Path("/")
    public interface NestedApi {
        @GET @Produces(TEXT_PLAIN) String fake();
    }

    @Inject @RestClient NestedApi nestedApi;

    @Inject S3 s3;

    private final Map<String, Runnable> objectCreatedHandlers = new ConcurrentHashMap<>();

    void observe(@Observes ObjectCreatedEvent event) {
        log.info("observed: {}", event);
        Optional.ofNullable(objectCreatedHandlers.remove(event.key()))
                .ifPresentOrElse(Runnable::run, () -> log.warn("no runnable for {}", event));
    }


    @GET
    @Produces(TEXT_PLAIN)
    public String hello() {return "Hello World!";}

    @GET @Path("/fake")
    @Produces(TEXT_PLAIN)
    public String fake() {return "Hello, " + nestedApi.fake();}

    @GET @Path("/s3")
    @Produces(TEXT_PLAIN)
    public String s3() {return "Hello, " + s3.getTextObject(BUCKET_NAME, "s3.txt");}

    @POST @Path("/s3")
    @Consumes(TEXT_PLAIN)
    @Produces(TEXT_PLAIN)
    public void post(String body, @Suspended AsyncResponse response) {
        var id = Instant.now().toString();
        log.info("put text object to S3 bucket `{}` object `{}`: `{}`", BUCKET_NAME, id, body);

        objectCreatedHandlers.put(BUCKET_NAME + "/" + id, () -> {
            log.info("object `{}` was created", id);
            var read = s3.getTextObject(BUCKET_NAME, id);
            if (!read.equals(body)) throw new RuntimeException("expected `" + body + "` but got `" + read + "`");
            s3.removeObject(BUCKET_NAME, id);
            response.resume("got:" + read);
        });

        s3.putTextObject(BUCKET_NAME, id, body);
    }
}
