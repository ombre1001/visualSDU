package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reasonCode;
    private String description;
    private Integer status;
    private String decisionReason;
    private Long processedBy;
    private LocalDateTime processedAt;
    private Integer version;
    private String submitIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer activeMarker;
}
