package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 合并目标标签。 */
public record AdminMergeTagRequest(
        @NotNull(message = "目标标签ID不能为空")
        @Positive(message = "目标标签ID必须为正数")
        Long targetTagId
) {
}
