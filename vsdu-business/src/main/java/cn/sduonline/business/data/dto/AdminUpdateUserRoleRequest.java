package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** role 只允许：0=正式用户，1=管理员。 */
public record AdminUpdateUserRoleRequest(
        @NotNull(message = "角色不能为空")
        @Min(value = 0, message = "role只能为0或1")
        @Max(value = 1, message = "role只能为0或1")
        Integer role
) {
}
