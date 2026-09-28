# WHOOP Java SDK

An unofficial Java client for the [WHOOP Developer Platform](https://developer.whoop.com/api/) (API v2).

Wraps OAuth, pagination, retries, rate-limit handling and webhook verification so your application
deals in `Recovery` and `Sleep` objects instead of HTTP.

- **Java 17+**, no framework required
- **Two dependencies**: Jackson databind + JSR-310 (HTTP uses the JDK's built-in `HttpClient`)
- **Thread-safe** — build one `WhoopClient` per authorized user and keep it

## Install

### From GitHub Packages

Released versions are published to this repository's package registry by
[`.github/workflows/publish.yml`](.github/workflows/publish.yml).

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/HuseinJ/whoopSDK</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.whoopsdk</groupId>
  <artifactId>whoop-java-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

### From source

```bash
./mvnw install     # installs 0.1.0-SNAPSHOT into ~/.m2
```

## Quick start

```java
WhoopClient whoop = WhoopClient.withAccessToken(accessToken);

UserBasicProfile me = whoop.users().getProfile();
System.out.println(me.fullName());

// Last 7 days of recovery scores.
whoop.recoveries()
     .streamRecoveries(PageRequest.since(Instant.now().minus(7, ChronoUnit.DAYS)))
     .filter(r -> r.scoreState().isScored())
     .forEach(r -> System.out.printf("cycle %d: %.0f%% recovered%n",
             r.cycleId(), r.score().recoveryScore()));
```

## Authentication

### You already have a token

```java
WhoopClient whoop = WhoopClient.withAccessToken(token);
```

Access tokens expire after about an hour. For anything longer-lived, use the refreshing provider
below.

### Full authorization-code flow

```java
var oauth = new WhoopOAuthClient(clientId, clientSecret);

// 1. Redirect the user to WHOOP. Persist `state` against their session.
String state = WhoopOAuthClient.randomState();
String url = oauth.authorizationUrl(
        "https://yourapp.example/callback",
        Set.of(WhoopScope.READ_RECOVERY, WhoopScope.READ_SLEEP, WhoopScope.OFFLINE),
        state);

// 2. WHOOP redirects back with ?code=...&state=... — compare state, then exchange the code.
OAuthTokens tokens = oauth.exchangeCode(code, "https://yourapp.example/callback");
tokenRepository.save(userId, tokens);

// 3. Build a client that renews the token by itself.
var provider = new RefreshingTokenProvider(oauth, tokens,
        renewed -> tokenRepository.save(userId, renewed));   // persist the rotated refresh token

WhoopClient whoop = WhoopClient.builder().tokenProvider(provider).build();
```

Two things to get right:

- Request `WhoopScope.OFFLINE`, or the response carries no refresh token and the user must
  re-authorize every hour.
- **WHOOP rotates refresh tokens on every refresh.** The `onTokensRenewed` callback is where you
  persist the new pair; skip it and a process restart leaves you unable to refresh.

### Scopes

| Scope                   | Constant                | Unlocks                        |
| ----------------------- | ----------------------- | ------------------------------ |
| `read:profile`          | `READ_PROFILE`          | `users().getProfile()`         |
| `read:body_measurement` | `READ_BODY_MEASUREMENT` | `users().getBodyMeasurement()` |
| `read:cycles`           | `READ_CYCLES`           | everything on `cycles()`       |
| `read:recovery`         | `READ_RECOVERY`         | everything on `recoveries()`   |
| `read:sleep`            | `READ_SLEEP`            | everything on `sleeps()`       |
| `read:workout`          | `READ_WORKOUT`          | everything on `workouts()`     |
| `offline`               | `OFFLINE`               | receiving a refresh token      |

A call made without its scope fails with `WhoopAuthException`.

## API surface

| Accessor             | Methods                                                                                |
| -------------------- | -------------------------------------------------------------------------------------- |
| `whoop.users()`      | `getProfile()`, `getBodyMeasurement()`, `revokeAccess()`                               |
| `whoop.cycles()`     | `getCycle(id)`, `listCycles(..)`, `streamCycles(..)`, `getSleepForCycle(id)`           |
| `whoop.recoveries()` | `listRecoveries(..)`, `streamRecoveries(..)`, `getRecoveryForCycle(id)`                |
| `whoop.sleeps()`     | `getSleep(uuid)`, `listSleeps(..)`, `streamSleeps(..)`, `streamNights(..)`             |
| `whoop.workouts()`   | `getWorkout(uuid)`, `listWorkouts(..)`, `streamWorkouts(..)`, `resolveV1WorkoutId(id)` |

A _cycle_ is WHOOP's day: it runs wake-to-wake rather than midnight-to-midnight, which is why
recovery is keyed by `cycleId` rather than by date.

### Score state

Records exist before they are scored. `score` is only populated when `scoreState()` is `SCORED`:

```java
Cycle cycle = whoop.cycles().getCycle(cycleId);

cycle.scoreOptional().ifPresentOrElse(
        score -> System.out.println("strain " + score.strain()),
        ()    -> System.out.println("not scored yet: " + cycle.scoreState()));
```

`PENDING_SCORE` means check back later; `UNSCORABLE` means the score will never arrive.

## Pagination

`PageRequest` covers `limit`, `start`, `end` and `nextToken`. `start` is inclusive, `end` exclusive,
and the API caps `limit` at 25.

```java
PageRequest.of(25)
PageRequest.since(Instant.now().minus(30, ChronoUnit.DAYS))
PageRequest.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))
```

Either page manually:

```java
Page<Workout> page = whoop.workouts().listWorkouts(PageRequest.of(25));
while (page.hasNext()) {
    process(page.records());
    page = whoop.workouts().listWorkouts(PageRequest.of(25).nextToken(page.nextToken()));
}
```

…or let the SDK walk the pages. The stream is lazy, so a bounded operation only fires the requests
it needs:

```java
List<Workout> recent = whoop.workouts()
        .streamWorkouts(PageRequest.of(25))
        .limit(10)          // fetches one page, not the user's whole history
        .toList();
```

## Rate limits

WHOOP allows **100 requests/minute** and **10,000/day** per client. The SDK retries 429 and 5xx
responses with exponential backoff, honouring `Retry-After` and `X-RateLimit-Reset`. Once the
retries are spent it throws `WhoopRateLimitException`.

For bulk imports, check the budget as you go:

```java
whoop.lastRateLimit()
     .flatMap(RateLimit::remainingRequests)
     .filter(remaining -> remaining < 10)
     .ifPresent(remaining -> pauseUntilWindowResets());
```

Tune the policy per client:

```java
WhoopClient whoop = WhoopClient.builder()
        .tokenProvider(provider)
        .config(WhoopConfig.builder()
                .maxRetries(5)
                .requestTimeout(Duration.ofSeconds(20))
                .maxRetryDelay(Duration.ofSeconds(60))
                .build())
        .build();
```

## Webhooks

WHOOP posts `recovery`, `sleep` and `workout` events to your registered URL. The payload carries
only an id and a type — fetch the record through the API if you need its contents. Creates arrive as
`*.updated`, so treat an update for an unseen id as a create.

```java
var verifier = new WhoopWebhookVerifier(clientSecret);

@PostMapping("/whoop/webhook")
ResponseEntity<Void> receive(@RequestBody String rawBody,
                             @RequestHeader("X-WHOOP-Signature") String signature,
                             @RequestHeader("X-WHOOP-Signature-Timestamp") String timestamp) {

    WebhookEvent event = verifier.verifyAndParse(rawBody, signature, timestamp);

    switch (event.eventType()) {
        case SLEEP_UPDATED   -> queue.submit(() -> syncSleep(event.idAsUuid().orElseThrow()));
        case WORKOUT_UPDATED -> queue.submit(() -> syncWorkout(event.idAsUuid().orElseThrow()));
        case RECOVERY_UPDATED-> queue.submit(() -> syncRecovery(event.idAsLong().orElseThrow()));
        default              -> {}
    }
    return ResponseEntity.ok().build();   // respond fast; WHOOP retries slow endpoints
}
```

The body must be the **exact bytes received**. If your framework parses the JSON first and you
re-serialize it, the signature will not match — take a `@RequestBody String` or `byte[]`.

`verifyAndParse` also rejects deliveries whose timestamp is more than five minutes old, which blocks
replays. Widen or disable that window with `new WhoopWebhookVerifier(secret, tolerance)`.

## Errors

All failures extend `WhoopException`, which is unchecked.

| Exception                 | Raised for                                                          |
| ------------------------- | ------------------------------------------------------------------- |
| `WhoopAuthException`      | 401/403, failed token exchange, bad webhook signature               |
| `WhoopRateLimitException` | 429 after retries are exhausted; carries `retryAfter()`             |
| `WhoopApiException`       | any other non-2xx; carries `statusCode()`, `body()`, `isNotFound()` |
| `WhoopException`          | transport failure, JSON that will not parse                         |

```java
try {
    Recovery recovery = whoop.recoveries().getRecoveryForCycle(cycleId);
} catch (WhoopApiException e) {
    if (e.isNotFound()) {
        return Optional.empty();   // no recovery scored for that cycle
    }
    throw e;
}
```

## Endpoints not wrapped

The trusted-partner endpoints (`/v2/partner/**`) need client-credentials auth and are not covered.
For those, or anything WHOOP adds later, drop to the transport:

```java
String json = whoop.http().request("GET", "/v2/some/new/endpoint", Map.of("limit", "10"), null);
```

## Project layout

```
.github/workflows/            ci.yml (test on 17 + 21), publish.yml (release → Packages)
src/main/java/com/whoopsdk/
├── WhoopClient.java          entry point
├── WhoopConfig.java          base URLs, timeouts, retry policy
├── api/                      UserApi, CycleApi, RecoveryApi, SleepApi, WorkoutApi
├── auth/                     OAuth flow, token providers, scopes
├── http/                     transport, retries, rate limits, Jackson setup
├── model/                    records mirroring the API schemas
├── pagination/               lazy page walking
├── webhook/                  signature verification and event parsing
└── exception/                exception hierarchy
```

## Development

```bash
./mvnw test      # 38 tests, no network — the HTTP tests run against an in-process server
./mvnw package   # jar + sources + javadoc
```

CI runs the suite on Java 17 and 21 for every push and pull request.

## Releasing

The pom stays at `-SNAPSHOT`; the published version comes from the release tag, so there is no
version-bump commit.

1. Draft a GitHub Release with a tag like `v0.1.0` (the leading `v` is stripped).
2. Publishing it runs the test suite, deploys to GitHub Packages, and attaches the three jars.

To publish without cutting a release, run the **Publish package** workflow manually and pass a
version.

The workflow overrides the pom's `github.repository` property with the repository it runs in, so a
fork publishes to its own registry with no edits. If you deploy from a laptop instead, set the
property yourself:

```bash
./mvnw deploy -Dgithub.repository=OWNER/whoopSDK
```

**Versions are immutable.** GitHub Packages answers a re-publish of an existing version with 409
Conflict, so every release needs a new number. Deleting and re-pushing the same version is possible
through the package settings UI but breaks anyone who already resolved it.

## Notes

Not affiliated with or endorsed by WHOOP. Built against API v2; the v1 endpoints are deprecated and
only `resolveV1WorkoutId` touches them, for migrating stored v1 ids.
