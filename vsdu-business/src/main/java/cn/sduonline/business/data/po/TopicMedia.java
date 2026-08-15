package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("topic_media")
public class TopicMedia {

    private Long topicId;

    private Long mediaId;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}