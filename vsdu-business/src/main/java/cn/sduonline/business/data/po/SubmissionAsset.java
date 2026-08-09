package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission_asset")
public class SubmissionAsset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private String objectKey;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private Integer sortOrder;
    private Long mediaId;
    private LocalDateTime createdAt;
}
