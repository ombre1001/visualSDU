package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.FavoriteBatchAction;

public record BatchFavoriteResultVO(
        FavoriteBatchAction action,
        int requestedCount,
        int affectedCount,
        Long folderId
) {
}