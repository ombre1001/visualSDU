package cn.sduonline.business.data.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminReportDetailRow {
    private Long id;
    private Long reporterId;
    private String reporterCasId;
    private String reporterName;
    private String reporterNickname;
    private String reporterAvatarKey;
    private Long reporterReportCount;
    private Long reporterConfirmedCount;
    private String targetType;
    private Long targetId;
    private Boolean targetExists;
    private String targetTitle;
    private String targetDescription;
    private Long targetUploaderId;
    private Integer targetStatus;
    private String targetThumbnailKey;
    private String reasonCode;
    private String reasonName;
    private String reasonDescription;
    private String description;
    private Integer status;
    private String decisionReason;
    private Long processedBy;
    private String processorName;
    private LocalDateTime processedAt;
    private Long relatedActiveReportCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
