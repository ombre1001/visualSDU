package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MediaDetailVO(
        Long id,
        Long uploaderId,
        String uploaderNickname,
        Long locationId,
        String locationName,
        String title,
        String description,
        String imageUrl,
        String thumbnailUrl,
        LocalDateTime shotAt,
        List<String> tags,
        long viewCount,
        long likeCount,
        long favoriteCount,
        long downloadCount,
        boolean liked,
        boolean favorited,
        LocalDateTime createdAt
) {
}
