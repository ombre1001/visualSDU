package cn.sduonline.business.data.vo;

import java.util.List;

public record DiscoveryCampusSectionVO(
        Long campusId,
        String campusName,
        String coverUrl,
        List<MediaSummaryVO> media
) {
}