package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AdminTopicVO(
        Long id, String name, String slug, String description, String coverUrl,
        Integer status, Integer sortOrder, List<Long> mediaIds,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
