package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.SubmissionStatus;

import java.time.LocalDateTime;

public record SubmissionSummaryVO(
        Long id,
        Long locationId,
        String locationName,
        String description,
        LocalDateTime shotAt,
        SubmissionStatus status,
        String reviewReason,
        int assetCount,
        String coverUrl,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt
) {
}
