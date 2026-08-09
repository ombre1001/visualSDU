package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("media")
public class Media {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long uploaderId;
    private Long locationId;
    private String objectKey;
    private String thumbnailKey;
    private String title;
    private String description;
    private LocalDateTime shotAt;
    private String tags;
    private Integer status;
    private Long viewCount;
    private Long likeCount;
    private Long favoriteCount;
    private Long downloadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
