package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record AdminCreateTopicRequest(
        @NotBlank(message = "专题名称不能为空") @Size(max = 100, message = "专题名称不能超过100个字符") String name,
        @NotBlank(message = "专题标识不能为空")
        @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,63}", message = "专题标识只能包含小写字母、数字和连字符") String slug,
        @Size(max = 1000, message = "专题描述不能超过1000个字符") String description,
        @Size(max = 1000, message = "封面地址不能超过1000个字符") String coverUrl,
        @Min(value = 0, message = "status只能为0或1") @Max(value = 1, message = "status只能为0或1") Integer status,
        @PositiveOrZero(message = "排序值不能为负数") Integer sortOrder,
        @Size(max = 200, message = "专题最多关联200个媒体")
        List<@NotNull(message = "媒体ID不能为空") @Positive(message = "媒体ID必须为正数") Long> mediaIds
) {
}
