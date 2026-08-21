package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateTagRequest(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 32, message = "标签名称不能超过32个字符")
        String name
) {
}
