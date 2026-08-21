package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAnnouncementRequest(
        @NotBlank(message = "公告标题不能为空")
        @Size(max = 200, message = "公告标题不能超过200个字符")
        String title,

        @Size(max = 500, message = "公告摘要不能超过500个字符")
        String summary,

        @NotBlank(message = "公告正文不能为空")
        @Size(max = 50000, message = "公告正文不能超过50000个字符")
        String content,

        Boolean isPinned,

        @Min(value = 0, message = "公告排序值不能小于0")
        Integer sortOrder
) {
}
