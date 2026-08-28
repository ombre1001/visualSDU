package cn.sduonline.business.data.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminReportOperationLogRow {
    private Long id;
    private Long reportId;
    private Long operatorId;
    private String operatorName;
    private String operationType;
    private Integer decision;
    private Integer beforeStatus;
    private Integer afterStatus;
    private String reason;
    private String actionsJson;
    private String resultJson;
    private Integer reportVersion;
    private LocalDateTime createdAt;
}
