package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSubmissionSummaryVO(
        Long id, Long userId, String uploaderName,
        Long locationId, String locationName, String description,
        LocalDateTime shotAt, List<String> tags, SubmissionStatus status,
        String reviewReason, int assetCount, String coverUrl,
        LocalDateTime submittedAt, LocalDateTime reviewedAt,
        LocalDateTime updatedAt, Integer version
) {
}
