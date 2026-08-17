package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record BrowseHistoryVO(
        MediaSummaryVO media,
        long viewCount,
        LocalDateTime lastViewedAt
) {
}
