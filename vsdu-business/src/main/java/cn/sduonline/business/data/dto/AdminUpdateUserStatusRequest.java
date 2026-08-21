package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** status：0=停用，1=正常，2=冻结。 */
public record AdminUpdateUserStatusRequest(
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "status只能为0、1或2")
        @Max(value = 2, message = "status只能为0、1或2")
        Integer status,

        LocalDateTime frozenUntil,

        @Size(max = 255, message = "冻结原因不能超过255个字符")
        String frozenReason
) {
}
