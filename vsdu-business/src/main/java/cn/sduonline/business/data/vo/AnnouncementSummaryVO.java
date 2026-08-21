package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record AnnouncementSummaryVO(
        Long id,
        String title,
        String summary,
        Boolean isPinned,
        LocalDateTime publishedAt
) {
}
