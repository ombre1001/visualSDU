package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "refresh token不能为空")
        String refreshToken
) {
}
