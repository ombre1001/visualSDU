package cn.sduonline.business.data.projection;

import cn.sduonline.business.data.enums.SubmissionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminSubmissionDetailRow {
    private Long id;
    private Long userId;
    private String uploaderCasId;
    private String uploaderName;
    private String uploaderNickname;
    private String uploaderAvatarKey;
    private Long uploaderSubmissionCount;
    private Long uploaderApprovedCount;
    private Long locationId;
    private String locationName;
    private String description;
    private LocalDateTime shotAt;
    private String tags;
    private SubmissionStatus status;
    private String reviewReason;
    private LocalDateTime submittedAt;
    private Long reviewedBy;
    private String reviewerName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
