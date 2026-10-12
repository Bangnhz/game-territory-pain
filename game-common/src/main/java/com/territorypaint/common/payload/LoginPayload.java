package com.territorypaint.common.payload;

/**
 * Login request payload containing JWT token from Backend.
 * Requirements: 13.1, 13.2, 20.1
 */
public class LoginPayload {
    private String jwt;
    private String accessToken;

    public LoginPayload() {
    }

    public LoginPayload(String jwt) {
        this.jwt = jwt;
        this.accessToken = jwt;
    }

    public String getJwt() {
        return jwt != null ? jwt : accessToken;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
        this.accessToken = jwt;
    }

    public String getAccessToken() {
        return accessToken != null ? accessToken : jwt;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.jwt = accessToken;
    }
}
