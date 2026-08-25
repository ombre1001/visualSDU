package cn.sduonline.business.data.projection;

import cn.sduonline.business.data.enums.SubmissionReviewDecision;
import cn.sduonline.business.data.enums.SubmissionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminSubmissionReviewLogRow {
    private Long id;
    private Long submissionId;
    private Integer roundNo;
    private Integer submissionVersion;
    private SubmissionReviewDecision decision;
    private String reason;
    private SubmissionStatus beforeStatus;
    private SubmissionStatus afterStatus;
    private Long reviewedBy;
    private String reviewerName;
    private LocalDateTime reviewedAt;
}
