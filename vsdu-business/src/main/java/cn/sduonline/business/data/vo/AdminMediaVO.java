package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AdminMediaVO(
        Long id, Long submissionId, Long uploaderId, Long locationId,
        String imageUrl, String thumbnailUrl, String title, String description,
        LocalDateTime shotAt, List<String> tags, Integer status,
        long viewCount, long likeCount, long favoriteCount, long downloadCount,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
