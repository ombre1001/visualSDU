package cn.sduonline.business.data.dto;

import cn.sduonline.business.data.enums.SubmissionReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AdminBatchReviewSubmissionItemRequest(
        @NotNull(message = "稿件ID不能为空")
        @Positive(message = "稿件ID必须为正数")
        Long submissionId,
        @NotNull(message = "审核决定不能为空")
        SubmissionReviewDecision decision,
        @Size(max = 1000, message = "审核原因不能超过1000个字符")
        String reason,
        @NotNull(message = "稿件版本不能为空")
        @PositiveOrZero(message = "稿件版本不能为负数")
        Integer expectedVersion
) {
    public AdminReviewSubmissionRequest toReviewRequest() {
        return new AdminReviewSubmissionRequest(decision, reason, expectedVersion);
    }
}
