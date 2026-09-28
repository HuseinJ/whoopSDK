package com.whoopsdk.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The event types WHOOP delivers to a registered webhook URL.
 *
 * <p>Creates are published as {@code *.updated} events — there is no separate "created" type, so
 * treat an update for an id you have not seen as a create.
 */
public enum WebhookEventType {

    RECOVERY_UPDATED("recovery.updated"),
    RECOVERY_DELETED("recovery.deleted"),
    SLEEP_UPDATED("sleep.updated"),
    SLEEP_DELETED("sleep.deleted"),
    WORKOUT_UPDATED("workout.updated"),
    WORKOUT_DELETED("workout.deleted"),
    /** An event type this SDK version does not know about; inspect {@link WebhookEvent#type()}. */
    UNKNOWN("unknown");

    private final String value;

    WebhookEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static WebhookEventType fromJson(String value) {
        if (value != null) {
            for (WebhookEventType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
        }
        return UNKNOWN;
    }

    public boolean isDelete() {
        return value.endsWith(".deleted");
    }

    @Override
    public String toString() {
        return value;
    }
}
