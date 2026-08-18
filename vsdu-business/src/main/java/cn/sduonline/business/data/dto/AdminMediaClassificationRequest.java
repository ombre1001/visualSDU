package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminMediaClassificationRequest(
        @Positive(message = "地点ID必须为正数") Long locationId,
        @Size(max = 20, message = "媒体最多关联20个标签")
        List<@NotNull(message = "标签ID不能为空") @Positive(message = "标签ID必须为正数") Long> tagIds
) {
}
