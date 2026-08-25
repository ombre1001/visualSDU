package cn.sduonline.business.data.po;

import cn.sduonline.business.data.enums.SubmissionReviewDecision;
import cn.sduonline.business.data.enums.SubmissionStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission_review_log")
public class SubmissionReviewLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Integer roundNo;
    private Integer submissionVersion;
    private SubmissionReviewDecision decision;
    private String reason;
    private SubmissionStatus beforeStatus;
    private SubmissionStatus afterStatus;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
}
