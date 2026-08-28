package cn.sduonline.business.data.dto;

import cn.sduonline.business.data.enums.ReportDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminReportDecisionRequest(
        @NotNull(message = "举报处理决定不能为空")
        ReportDecision decision,
        @Size(max = 1000, message = "举报处理理由不能超过1000个字符")
        String reason,
        @Size(max = 10, message = "单次最多执行10个举报处置动作")
        List<@Valid AdminReportDecisionActionRequest> actions,
        @NotNull(message = "举报版本不能为空")
        @PositiveOrZero(message = "举报版本不能为负数")
        Integer expectedVersion
) {
}
