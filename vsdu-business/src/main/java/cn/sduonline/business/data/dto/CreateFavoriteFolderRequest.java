package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFavoriteFolderRequest(

        @NotBlank(message = "收藏夹名称不能为空")
        @Size(max = 50, message = "收藏夹名称不能超过50个字符")
        String name,

        @Size(max = 255, message = "收藏夹描述不能超过255个字符")
        String description,

        @Min(value = 0, message = "排序值不能小于0")
        Integer sortOrder
) {
}
