package cn.sduonline.business.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminBatchReviewSubmissionsRequest(
        @NotEmpty(message = "批量审核项不能为空")
        @Size(max = 50, message = "单次最多审核50条稿件")
        List<@Valid AdminBatchReviewSubmissionItemRequest> items
) {
}
