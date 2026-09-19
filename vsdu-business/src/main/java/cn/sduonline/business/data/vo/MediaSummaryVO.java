package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MediaSummaryVO(
        Long id,
        String title,
        Long locationId,
        String locationName,
        String thumbnailUrl,
        LocalDateTime shotAt,
        List<TagVO> tags,
        long viewCount,
        long likeCount,
        long favoriteCount
) {
}
