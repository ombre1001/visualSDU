package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.ReportStatus;
import cn.sduonline.business.data.enums.ReportTargetType;

import java.time.LocalDateTime;

public record AdminReportSummaryVO(
        Long id,
        Long reporterId,
        String reporterName,
        ReportTargetType targetType,
        Long targetId,
        String targetTitle,
        String targetThumbnailUrl,
        String reasonType,
        String reasonName,
        String description,
        ReportStatus status,
        Long processedBy,
        String processorName,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        Integer version
) {
}
