package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 2, max = 30, message = "昵称长度必须在2到30个字符之间")
        String nickname,

        @Size(max = 500, message = "个人简介不能超过500个字符")
        String bio
) {
}
