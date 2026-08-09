package cn.sduonline.business.data.vo;

import java.util.List;

public record TimeComparisonSummaryVO(
        Long id,
        Long locationId,
        String locationName,
        String title,
        String description,
        List<MediaSummaryVO> media
) {
}
