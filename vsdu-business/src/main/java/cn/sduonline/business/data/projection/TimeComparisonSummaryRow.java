package cn.sduonline.business.data.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimeComparisonSummaryRow {
    private Long comparisonId;
    private Long comparisonLocationId;
    private String comparisonLocationName;
    private String comparisonTitle;
    private String comparisonDescription;
    private LocalDateTime comparisonUpdatedAt;
    private Long itemId;
    private Integer itemSortOrder;
    private Long mediaId;
    private String mediaTitle;
    private Long mediaLocationId;
    private String mediaLocationName;
    private String mediaThumbnailKey;
    private LocalDateTime mediaShotAt;
    private Long mediaViewCount;
    private Long mediaLikeCount;
    private Long mediaFavoriteCount;
}
