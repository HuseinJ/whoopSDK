package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The authorized user's identity. Requires the {@code read:profile} scope.
 *
 * @param userId    the WHOOP user id, which appears as {@code user_id} on every other record
 * @param email     the user's email address
 * @param firstName given name
 * @param lastName  family name
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserBasicProfile(
        long userId,
        String email,
        String firstName,
        String lastName) {

    @JsonIgnore
    public String fullName() {
        return (firstName + " " + lastName).trim();
    }
}
