package cn.sduonline.business.data.projection;

import cn.sduonline.business.data.enums.SubmissionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionSummaryRow {
    private Long id;
    private Long locationId;
    private String locationName;
    private String description;
    private LocalDateTime shotAt;
    private SubmissionStatus status;
    private String reviewReason;
    private Integer assetCount;
    private String coverKey;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
