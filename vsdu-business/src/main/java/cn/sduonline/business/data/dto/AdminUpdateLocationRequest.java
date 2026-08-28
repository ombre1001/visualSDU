package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AdminUpdateLocationRequest(
        @Positive(message = "校区ID必须为正数") Long campusId,
        @Size(min = 1, max = 100, message = "地点名称长度必须为1到100个字符") String name,
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$", message = "地点分类代码格式不正确")
        String categoryCode,
        @Size(max = 255, message = "地址不能超过255个字符") String address,
        @DecimalMin(value = "-180") @DecimalMax(value = "180") BigDecimal longitude,
        @DecimalMin(value = "-90") @DecimalMax(value = "90") BigDecimal latitude,
        @Size(max = 512, message = "封面Key不能超过512个字符") String coverKey,
        @Size(max = 2000, message = "地点描述不能超过2000个字符") String description,
        @PositiveOrZero(message = "排序值不能为负数") Integer sortOrder,
        @Min(value = 0, message = "status只能为0或1") @Max(value = 1, message = "status只能为0或1") Integer status
) {
}
