package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.SubmissionStatus;

public record AdminBatchReviewItemResultVO(
        Long submissionId, boolean success, Integer code, String message,
        SubmissionStatus status, Integer version
) {
}
