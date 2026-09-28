package com.whoopsdk.webhook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;
import java.util.UUID;

/**
 * A webhook notification from WHOOP.
 *
 * <p>The payload carries only an id and a type — fetch the record itself through the matching API
 * if you need its contents.
 *
 * @param userId  the WHOOP user the event concerns
 * @param id      the affected resource id: a UUID for sleep/workout in v2, a number for recovery
 *                (which is keyed by cycle id)
 * @param type    the {@code type} string exactly as sent; {@link #eventType()} parses it
 * @param traceId correlation id for the triggering event; log it when debugging with WHOOP support
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookEvent(
        @JsonProperty("user_id") long userId,
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("trace_id") String traceId) {

    /**
     * The parsed event type, or {@link WebhookEventType#UNKNOWN} for a type this SDK version does
     * not model — check {@link #type()} in that case.
     */
    @JsonIgnore
    public WebhookEventType eventType() {
        return WebhookEventType.fromJson(type);
    }

    /** The id as a UUID — present for sleep and workout events in API v2. */
    @JsonIgnore
    public Optional<UUID> idAsUuid() {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** The id as a number — recovery events reference a cycle id. */
    @JsonIgnore
    public Optional<Long> idAsLong() {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(id));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
