package cn.sduonline.business.data.dto;

import cn.sduonline.business.data.enums.AnnouncementStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAnnouncementStatusRequest(
        @NotNull(message = "公告状态不能为空")
        AnnouncementStatus status
) {
}
