package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "sdupass JWT不能为空")
        String sduPassJwt,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度必须在6到32个字符之间")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&._-]+$",
                message = "密码必须至少包含字母和数字，且只能包含字母、数字和常见特殊字符"
        )
        String password,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {
}
