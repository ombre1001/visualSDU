package cn.sduonline.business.data.vo;

public record TopicSummaryVO(
        Long id,
        String name,
        String slug,
        String description,
        String coverUrl,
        long mediaCount
) {
}