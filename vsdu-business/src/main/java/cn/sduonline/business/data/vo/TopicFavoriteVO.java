package cn.sduonline.business.data.vo;

public record TopicFavoriteVO(
        Long topicId,
        long favoriteCount,
        boolean favorited
) {
}
