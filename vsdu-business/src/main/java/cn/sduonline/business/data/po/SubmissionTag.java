package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission_tag")
public class SubmissionTag {
    private Long submissionId;
    private Long tagId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
