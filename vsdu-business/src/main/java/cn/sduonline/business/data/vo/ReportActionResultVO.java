package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.ReportActionType;

public record ReportActionResultVO(
        ReportActionType type,
        Long targetId,
        String message
) {
}
