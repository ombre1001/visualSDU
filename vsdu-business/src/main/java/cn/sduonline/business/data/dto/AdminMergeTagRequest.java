package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Positive;

/** targetTagId 为空表示删除标签；非空表示合并到目标标签。 */
public record AdminMergeTagRequest(
        @Positive(message = "目标标签ID必须为正数")
        Long targetTagId
) {
}
