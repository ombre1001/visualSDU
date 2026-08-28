package cn.sduonline.business.data.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminReportSummaryRow {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private String targetType;
    private Long targetId;
    private String targetTitle;
    private String targetThumbnailKey;
    private String reasonCode;
    private String reasonName;
    private String description;
    private Integer status;
    private Long processedBy;
    private String processorName;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private Integer version;
}
