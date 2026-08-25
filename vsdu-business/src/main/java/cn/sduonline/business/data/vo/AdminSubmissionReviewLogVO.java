package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.SubmissionReviewDecision;
import cn.sduonline.business.data.enums.SubmissionStatus;

import java.time.LocalDateTime;

public record AdminSubmissionReviewLogVO(
        Long id, Long submissionId, Integer roundNo, Integer submissionVersion,
        SubmissionReviewDecision decision, String reason,
        SubmissionStatus beforeStatus, SubmissionStatus afterStatus,
        Long reviewedBy, String reviewerName, LocalDateTime reviewedAt
) {
}
