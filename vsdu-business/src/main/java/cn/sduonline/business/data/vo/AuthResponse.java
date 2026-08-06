package cn.sduonline.business.data.vo;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
