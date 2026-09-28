package com.whoopsdk.api;

import com.whoopsdk.http.WhoopHttpClient;
import com.whoopsdk.model.UserBasicProfile;
import com.whoopsdk.model.UserBodyMeasurement;

import java.util.Map;
import java.util.Objects;

/** Endpoints under {@code /v2/user}. Reached via {@code whoop.users()}. */
public final class UserApi {

    private final WhoopHttpClient http;

    public UserApi(WhoopHttpClient http) {
        this.http = Objects.requireNonNull(http);
    }

    /**
     * The authorized user's name, email and id.
     *
     * <p>Requires the {@code read:profile} scope.
     */
    public UserBasicProfile getProfile() {
        return http.get("/v2/user/profile/basic", Map.of(), UserBasicProfile.class);
    }

    /**
     * The authorized user's height, weight and max heart rate.
     *
     * <p>Requires the {@code read:body_measurement} scope.
     */
    public UserBodyMeasurement getBodyMeasurement() {
        return http.get("/v2/user/measurement/body", Map.of(), UserBodyMeasurement.class);
    }

    /**
     * Revokes the access token currently in use, ending the app's access for this user.
     *
     * <p>Irreversible: the user must go through the authorization flow again. After this call the
     * client instance is no longer usable.
     */
    public void revokeAccess() {
        http.delete("/v2/user/access");
    }
}
