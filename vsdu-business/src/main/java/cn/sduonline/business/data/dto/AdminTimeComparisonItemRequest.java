package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AdminTimeComparisonItemRequest(
        @NotNull(message = "媒体ID不能为空") @Positive(message = "媒体ID必须为正数") Long mediaId,
        @Size(max = 100, message = "对比项标签不能超过100个字符") String label,
        @PositiveOrZero(message = "排序值不能为负数") Integer sortOrder
) {
}
