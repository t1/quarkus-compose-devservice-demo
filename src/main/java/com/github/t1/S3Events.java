package com.github.t1;

import com.github.t1.S3.BucketAlreadyOwnedByYouException;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static com.github.t1.HelloResource.BUCKET_NAME;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Slf4j
@Consumes(APPLICATION_JSON)
@Path("/s3/events")
public class S3Events {
    @Inject com.github.t1.S3 s3;

    @Inject
    Event<ObjectCreatedEvent> objectCreatedEvents;

    @Inject @ConfigProperty(name = "s3.event-queue") String eventQueue;

    @Startup
    void init() {
        log.info("create S3 bucket");
        try {
            s3.createBucket(BUCKET_NAME);
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("bucket already exists: {}", BUCKET_NAME);
        }

        // see https://github.com/minio/minio/tree/master/docs/bucket/notifications#step-2-enable-webhook-bucket-notification-using-minio-client
        log.info("init S3 bucket event listener");
        s3.addEventListener(BUCKET_NAME, eventQueue);
    }

    @POST
    public Response onS3Notification(@Valid @NotNull S3Event event) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (event.EventName) {
            case "s3:ObjectCreated:Put" -> handleObjectCreated(event.Key);
            default -> log.info("ignoring event of type {}", event.EventName);
        }
        return Response.accepted().build();
    }

    public record S3Event(String EventName, String Key) {}

    public record ObjectCreatedEvent(String key) {}

    private void handleObjectCreated(String key) {
        var event = new ObjectCreatedEvent(key);
        log.info("fire {}", event);
        objectCreatedEvents.fire(event);
    }
}
