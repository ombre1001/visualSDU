package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record TopicDetailVO(
        Long id,
        String name,
        String slug,
        String description,
        String coverUrl,
        long mediaCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}