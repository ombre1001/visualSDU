package cn.sduonline.business.data.vo;

public record MediaInteractionVO(
        Long mediaId,
        long viewCount,
        long likeCount,
        long favoriteCount,
        boolean liked,
        boolean favorited
) {
}
