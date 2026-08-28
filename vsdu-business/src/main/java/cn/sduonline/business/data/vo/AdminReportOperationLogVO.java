package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.ReportDecision;
import cn.sduonline.business.data.enums.ReportStatus;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record AdminReportOperationLogVO(
        Long id,
        String operationType,
        ReportDecision decision,
        ReportStatus beforeStatus,
        ReportStatus afterStatus,
        String reason,
        JsonNode actions,
        JsonNode results,
        Long operatorId,
        String operatorName,
        Integer reportVersion,
        LocalDateTime createdAt
) {
}
