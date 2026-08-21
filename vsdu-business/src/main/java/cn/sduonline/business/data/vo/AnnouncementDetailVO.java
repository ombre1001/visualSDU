package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record AnnouncementDetailVO(
        Long id,
        String title,
        String summary,
        String content,
        Boolean isPinned,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
