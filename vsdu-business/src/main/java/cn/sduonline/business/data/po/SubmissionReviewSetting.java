package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission_review_setting")
public class SubmissionReviewSetting {
    @TableId(type = IdType.INPUT)
    private Integer id;
    private Boolean reviewEnabled;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}