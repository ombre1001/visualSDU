package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AdminCreateLocationRequest(
        @NotNull(message = "校区ID不能为空") @Positive(message = "校区ID必须为正数") Long campusId,
        @NotBlank(message = "地点名称不能为空") @Size(max = 100, message = "地点名称不能超过100个字符") String name,
        @NotBlank(message = "地点分类不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$", message = "地点分类代码格式不正确")
        String categoryCode,
        @Size(max = 255, message = "地址不能超过255个字符") String address,
        @NotNull(message = "经度不能为空") @DecimalMin(value = "-180") @DecimalMax(value = "180") BigDecimal longitude,
        @NotNull(message = "纬度不能为空") @DecimalMin(value = "-90") @DecimalMax(value = "90") BigDecimal latitude,
        @Size(max = 512, message = "封面Key不能超过512个字符") String coverKey,
        @Size(max = 2000, message = "地点描述不能超过2000个字符") String description,
        @PositiveOrZero(message = "排序值不能为负数") Integer sortOrder,
        @Min(value = 0, message = "status只能为0或1") @Max(value = 1, message = "status只能为0或1") Integer status
) {
}
