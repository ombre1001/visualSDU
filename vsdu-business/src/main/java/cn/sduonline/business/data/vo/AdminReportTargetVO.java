package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.ReportTargetType;

public record AdminReportTargetVO(
        ReportTargetType type,
        Long id,
        boolean exists,
        String title,
        String description,
        Long uploaderId,
        Integer status,
        String thumbnailUrl
) {
}
