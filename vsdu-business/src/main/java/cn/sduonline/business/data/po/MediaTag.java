package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("media_tag")
public class MediaTag {
    private Long mediaId;
    private Long tagId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
