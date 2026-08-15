package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateFavoriteFolderRequest(

        @Size(max = 50, message = "收藏夹名称不能超过50个字符")
        String name,

        @Size(max = 255, message = "收藏夹描述不能超过255个字符")
        String description,

        @Positive(message = "封面媒体ID必须为正数")
        Long coverMediaId,

        /**
         * 是否清除当前收藏夹封面。
         * true：清除封面；
         * false/null：不清除。
         */
        Boolean clearCover,

        @Min(value = 0, message = "排序值不能小于0")
        Integer sortOrder
) {
}
