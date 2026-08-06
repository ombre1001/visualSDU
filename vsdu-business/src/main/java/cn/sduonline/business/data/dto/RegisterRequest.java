package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "sdupass JWT不能为空")
        String sduPassJwt
) {
}
