package cn.sduonline.business.data.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MediaSummaryRow {
    private Long id;
    private String title;
    private Long locationId;
    private String locationName;
    private String thumbnailKey;
    private String tags;
    private LocalDateTime shotAt;
    private Long viewCount;
    private Long likeCount;
    private Long favoriteCount;
}
