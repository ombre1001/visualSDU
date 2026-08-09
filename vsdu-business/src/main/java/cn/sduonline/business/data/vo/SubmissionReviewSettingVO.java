package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record SubmissionReviewSettingVO(
        boolean reviewEnabled,
        Long updatedBy,
        LocalDateTime updatedAt
) {
}