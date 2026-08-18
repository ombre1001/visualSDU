package cn.sduonline.business.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record AdminCreateTimeComparisonRequest(
        @NotNull(message = "地点ID不能为空") @Positive(message = "地点ID必须为正数") Long locationId,
        @NotBlank(message = "时光对比标题不能为空") @Size(max = 200, message = "标题不能超过200个字符") String title,
        @Size(max = 2000, message = "描述不能超过2000个字符") String description,
        @Min(value = 0, message = "status只能为0或1") @Max(value = 1, message = "status只能为0或1") Integer status,
        @NotNull(message = "对比项不能为空") @Size(min = 2, max = 20, message = "时光对比必须包含2到20个媒体")
        List<@Valid AdminTimeComparisonItemRequest> items
) {
}
