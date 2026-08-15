package cn.sduonline.business.data.dto;

import cn.sduonline.business.data.enums.FavoriteBatchAction;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchFavoriteRequest(

        @NotNull(message = "批量操作类型不能为空")
        FavoriteBatchAction action,

        /**
         * ADD时表示要加入的收藏夹；
         * REMOVE时表示从哪个收藏夹移除；
         * MOVE时表示源收藏夹。
         */
        @Positive(message = "收藏夹ID必须为正数")
        Long folderId,

        /**
         * MOVE时的目标收藏夹。
         */
        @Positive(message = "目标收藏夹ID必须为正数")
        Long targetFolderId,

        @NotEmpty(message = "媒体ID列表不能为空")
        @Size(max = 100, message = "单次最多操作100个媒体")
        List<
                @NotNull(message = "媒体ID不能为空")
                @Positive(message = "媒体ID必须为正数")
                        Long
                > mediaIds
) {
}