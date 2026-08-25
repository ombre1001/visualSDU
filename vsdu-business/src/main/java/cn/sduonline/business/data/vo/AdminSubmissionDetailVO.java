package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSubmissionDetailVO(
        Long id, AdminSubmissionUploaderVO uploader,
        Long locationId, String locationName, String description,
        LocalDateTime shotAt, List<String> tags, SubmissionStatus status,
        String reviewReason, LocalDateTime submittedAt,
        Long reviewedBy, String reviewerName, LocalDateTime reviewedAt,
        LocalDateTime createdAt, LocalDateTime updatedAt, Integer version,
        List<SubmissionAssetVO> assets,
        List<AdminSubmissionReviewLogVO> recentReviewLogs
) {
}
