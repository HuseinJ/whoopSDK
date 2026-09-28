package com.whoopsdk.webhook;

import com.whoopsdk.exception.WhoopAuthException;
import com.whoopsdk.http.Json;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Verifies and parses incoming WHOOP webhooks.
 *
 * <p>WHOOP signs each delivery with
 * {@code base64(HMAC-SHA256(timestamp + rawBody, clientSecret))}. The body must be the exact bytes
 * received — if your framework parses JSON before you get to it, re-serializing will change the
 * bytes and the signature will not match. In Spring, take a {@code @RequestBody byte[]} or
 * {@code String}; in a servlet, read the input stream before anything else touches it.
 *
 * <pre>{@code
 * var verifier = new WhoopWebhookVerifier(clientSecret);
 *
 * @PostMapping("/whoop/webhook")
 * ResponseEntity<Void> receive(@RequestBody String body,
 *                              @RequestHeader("X-WHOOP-Signature") String signature,
 *                              @RequestHeader("X-WHOOP-Signature-Timestamp") String timestamp) {
 *     WebhookEvent event = verifier.verifyAndParse(body, signature, timestamp);
 *     // Respond 2xx quickly, then process asynchronously — WHOOP retries slow endpoints.
 *     queue.submit(event);
 *     return ResponseEntity.ok().build();
 * }
 * }</pre>
 */
public final class WhoopWebhookVerifier {

    public static final String SIGNATURE_HEADER = "X-WHOOP-Signature";
    public static final String TIMESTAMP_HEADER = "X-WHOOP-Signature-Timestamp";

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    /** Default window for replay protection. */
    private static final Duration DEFAULT_TOLERANCE = Duration.ofMinutes(5);

    private final byte[] clientSecret;
    private final Duration tolerance;

    /** @param clientSecret your WHOOP app's client secret — the same one used for OAuth */
    public WhoopWebhookVerifier(String clientSecret) {
        this(clientSecret, DEFAULT_TOLERANCE);
    }

    /**
     * @param tolerance how far the signature timestamp may drift from now before the delivery is
     *                  rejected as a replay; pass {@link Duration#ZERO} to skip the check
     */
    public WhoopWebhookVerifier(String clientSecret, Duration tolerance) {
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret")
                .getBytes(StandardCharsets.UTF_8);
        this.tolerance = Objects.requireNonNull(tolerance, "tolerance");
    }

    /**
     * Checks the signature without throwing.
     *
     * @param rawBody   the exact request body as received
     * @param signature value of the {@value #SIGNATURE_HEADER} header
     * @param timestamp value of the {@value #TIMESTAMP_HEADER} header (millis since epoch)
     */
    public boolean isValid(String rawBody, String signature, String timestamp) {
        if (rawBody == null || signature == null || timestamp == null) {
            return false;
        }
        if (!withinTolerance(timestamp)) {
            return false;
        }
        String expected = sign(rawBody, timestamp);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verifies the signature and deserializes the payload.
     *
     * @throws WhoopAuthException if the signature does not match or the timestamp is outside the
     *                            tolerance window
     */
    public WebhookEvent verifyAndParse(String rawBody, String signature, String timestamp) {
        if (!isValid(rawBody, signature, timestamp)) {
            throw new WhoopAuthException(
                    "WHOOP webhook signature verification failed. Make sure the body is the raw "
                            + "bytes received, not a re-serialized copy.");
        }
        return parse(rawBody);
    }

    /**
     * Deserializes a payload whose signature you have already checked.
     *
     * <p>Never call this on unverified input — anyone can POST to a public endpoint.
     */
    public WebhookEvent parse(String rawBody) {
        return Json.read(rawBody, WebhookEvent.class);
    }

    /** Computes the expected signature; exposed for testing and for signing fixtures. */
    public String sign(String rawBody, String timestamp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(clientSecret, HMAC_ALGORITHM));
            mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
            mac.update(rawBody.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(mac.doFinal());
        } catch (Exception e) {
            throw new WhoopAuthException("Failed to compute the webhook HMAC signature", e);
        }
    }

    private boolean withinTolerance(String timestamp) {
        if (tolerance.isZero()) {
            return true;
        }
        try {
            Instant sent = Instant.ofEpochMilli(Long.parseLong(timestamp.trim()));
            return Duration.between(sent, Instant.now()).abs().compareTo(tolerance) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
