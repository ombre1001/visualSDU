package cn.sduonline.business.data.vo;

import java.util.List;

public record DiscoveryHomeVO(
        Long cityId,
        List<MediaSummaryVO> featured,
        List<MediaSummaryVO> latest,
        List<PopularTagVO> popularTags,
        List<TopicSummaryVO> topics,
        List<DiscoveryCampusSectionVO> campuses
) {
}