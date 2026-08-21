package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.AnnouncementStatus;

import java.time.LocalDateTime;

public record AdminAnnouncementVO(
        Long id,
        String title,
        String summary,
        String content,
        AnnouncementStatus status,
        Boolean isPinned,
        Integer sortOrder,
        LocalDateTime publishedAt,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
