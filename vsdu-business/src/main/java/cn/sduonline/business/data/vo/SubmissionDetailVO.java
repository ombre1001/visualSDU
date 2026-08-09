package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionDetailVO(
        Long id,
        Long userId,
        Long locationId,
        String locationName,
        String description,
        LocalDateTime shotAt,
        List<String> tags,
        SubmissionStatus status,
        String reviewReason,
        LocalDateTime submittedAt,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SubmissionAssetVO> assets
) {
}
