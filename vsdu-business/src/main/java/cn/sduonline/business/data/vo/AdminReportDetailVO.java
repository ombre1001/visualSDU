package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReportDetailVO(
        Long id,
        AdminReportReporterVO reporter,
        AdminReportTargetVO target,
        String reasonType,
        String reasonName,
        String reasonDescription,
        String description,
        ReportStatus status,
        String decisionReason,
        Long processedBy,
        String processorName,
        LocalDateTime processedAt,
        long relatedActiveReportCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version,
        List<AdminReportOperationLogVO> operationLogs
) {
}
