package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.ReportStatus;
import cn.sduonline.business.data.enums.ReportTargetType;

import java.time.LocalDateTime;

public record ReportVO(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        String reasonType,
        String reasonName,
        String description,
        ReportStatus status,
        LocalDateTime createdAt,
        Integer version
) {
}
