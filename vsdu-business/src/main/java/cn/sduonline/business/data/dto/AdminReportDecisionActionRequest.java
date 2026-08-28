package cn.sduonline.business.data.dto;

import cn.sduonline.business.data.enums.ReportActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdminReportDecisionActionRequest(
        @NotNull(message = "举报处置动作类型不能为空")
        ReportActionType type,
        LocalDateTime frozenUntil,
        @Size(max = 255, message = "冻结原因不能超过255个字符")
        String reason
) {
}
