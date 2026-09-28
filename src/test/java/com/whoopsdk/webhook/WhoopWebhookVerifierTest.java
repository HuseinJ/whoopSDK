package com.whoopsdk.webhook;

import com.whoopsdk.exception.WhoopAuthException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhoopWebhookVerifierTest {

    private static final String SECRET = "my-client-secret";
    private static final String BODY = """
            {"user_id":456,"id":"550e8400-e29b-41d4-a716-446655440000",\
            "type":"sleep.updated","trace_id":"e369c784-5100-49e8-8098-75d35c47b31b"}""";

    private final WhoopWebhookVerifier verifier = new WhoopWebhookVerifier(SECRET);

    @Test
    void acceptsAGenuineSignature() {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String signature = verifier.sign(BODY, timestamp);

        assertTrue(verifier.isValid(BODY, signature, timestamp));
    }

    @Test
    void rejectsATamperedBody() {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String signature = verifier.sign(BODY, timestamp);

        assertFalse(verifier.isValid(BODY.replace("456", "999"), signature, timestamp));
    }

    @Test
    void rejectsASignatureFromAnotherSecret() {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String foreign = new WhoopWebhookVerifier("someone-elses-secret").sign(BODY, timestamp);

        assertFalse(verifier.isValid(BODY, foreign, timestamp));
    }

    @Test
    void rejectsAReplayedDelivery() {
        String oldTimestamp = String.valueOf(Instant.now().minus(Duration.ofHours(1)).toEpochMilli());
        String signature = verifier.sign(BODY, oldTimestamp);

        assertFalse(verifier.isValid(BODY, signature, oldTimestamp));
    }

    @Test
    void zeroToleranceDisablesTheReplayCheck() {
        WhoopWebhookVerifier lenient = new WhoopWebhookVerifier(SECRET, Duration.ZERO);
        String oldTimestamp = String.valueOf(Instant.now().minus(Duration.ofDays(30)).toEpochMilli());

        assertTrue(lenient.isValid(BODY, lenient.sign(BODY, oldTimestamp), oldTimestamp));
    }

    @Test
    void rejectsMissingHeaders() {
        assertFalse(verifier.isValid(BODY, null, "123"));
        assertFalse(verifier.isValid(BODY, "sig", null));
        assertFalse(verifier.isValid(BODY, "sig", "not-a-number"));
    }

    @Test
    void verifyAndParseReturnsTheEvent() {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        WebhookEvent event = verifier.verifyAndParse(BODY, verifier.sign(BODY, timestamp), timestamp);

        assertEquals(456L, event.userId());
        assertEquals(WebhookEventType.SLEEP_UPDATED, event.eventType());
        assertFalse(event.eventType().isDelete());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", event.idAsUuid().orElseThrow().toString());
        assertTrue(event.idAsLong().isEmpty());
        assertEquals("e369c784-5100-49e8-8098-75d35c47b31b", event.traceId());
    }

    @Test
    void verifyAndParseThrowsOnABadSignature() {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());

        assertThrows(WhoopAuthException.class,
                () -> verifier.verifyAndParse(BODY, "not-the-signature", timestamp));
    }

    @Test
    void parsesARecoveryEventWithANumericId() {
        WebhookEvent event = verifier.parse("""
                {"user_id":456,"id":93845,"type":"recovery.deleted","trace_id":"abc"}""");

        assertEquals(WebhookEventType.RECOVERY_DELETED, event.eventType());
        assertTrue(event.eventType().isDelete());
        assertEquals(93845L, event.idAsLong().orElseThrow());
        assertTrue(event.idAsUuid().isEmpty());
    }

    @Test
    void mapsAnUnknownEventTypeWithoutFailing() {
        WebhookEvent event = verifier.parse("""
                {"user_id":456,"id":"1","type":"nutrition.updated","trace_id":"abc"}""");

        assertEquals(WebhookEventType.UNKNOWN, event.eventType());
        assertEquals("nutrition.updated", event.type());
    }
}
