package cn.sduonline.business.data.vo;

public record LocationFavoriteVO(
        Long locationId,
        long favoriteCount,
        boolean favorited
) {
}
