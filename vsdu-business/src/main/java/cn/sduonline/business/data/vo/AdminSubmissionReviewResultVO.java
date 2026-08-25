package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.SubmissionStatus;

import java.time.LocalDateTime;

public record AdminSubmissionReviewResultVO(
        Long submissionId, SubmissionStatus status, Integer version,
        String reviewReason, Long reviewedBy, LocalDateTime reviewedAt
) {
}
