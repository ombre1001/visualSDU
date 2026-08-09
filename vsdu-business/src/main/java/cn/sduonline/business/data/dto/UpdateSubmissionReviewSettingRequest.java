package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSubmissionReviewSettingRequest(
        @NotNull(message = "审核开关不能为空") Boolean reviewEnabled
) {
}