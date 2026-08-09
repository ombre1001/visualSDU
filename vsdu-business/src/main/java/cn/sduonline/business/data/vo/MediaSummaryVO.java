package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record MediaSummaryVO(
        Long id,
        String title,
        Long locationId,
        String locationName,
        String thumbnailUrl,
        LocalDateTime shotAt,
        long viewCount,
        long likeCount,
        long favoriteCount
) {
}
