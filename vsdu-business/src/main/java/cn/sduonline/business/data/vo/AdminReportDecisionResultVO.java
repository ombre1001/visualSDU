package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReportDecisionResultVO(
        Long reportId,
        ReportStatus status,
        String reason,
        Long processedBy,
        LocalDateTime processedAt,
        Integer version,
        List<ReportActionResultVO> actions
) {
}
