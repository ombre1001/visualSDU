package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record TopicDetailVO(
        Long id,
        String name,
        String slug,
        String description,
        String coverUrl,
        long mediaCount,
        long favoriteCount,
        boolean favorited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
