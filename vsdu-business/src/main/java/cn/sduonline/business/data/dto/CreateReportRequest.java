package cn.sduonline.business.data.dto;

import cn.sduonline.business.data.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull(message = "举报目标类型不能为空")
        ReportTargetType targetType,
        @NotNull(message = "举报目标ID不能为空")
        @Positive(message = "举报目标ID必须为正数")
        Long targetId,
        @NotBlank(message = "举报理由不能为空")
        @Size(max = 32, message = "举报理由编码不能超过32个字符")
        String reasonType,
        @Size(max = 1000, message = "举报补充说明不能超过1000个字符")
        String description
) {
}
