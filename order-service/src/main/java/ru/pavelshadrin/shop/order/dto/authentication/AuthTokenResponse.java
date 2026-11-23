package ru.pavelshadrin.shop.order.dto.authentication;

public class AuthTokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long accessExpiresIn;
    private final long refreshExpiresIn;

    public AuthTokenResponse(String accessToken, String refreshToken,
                             long accessExpiresIn, long refreshExpiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.accessExpiresIn = accessExpiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getAccessExpiresIn() {
        return accessExpiresIn;
    }

    public long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }
}
