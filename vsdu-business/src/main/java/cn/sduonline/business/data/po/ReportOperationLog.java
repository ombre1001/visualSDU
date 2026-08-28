package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_operation_log")
public class ReportOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private Long operatorId;
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
