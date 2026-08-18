package cn.sduonline.business.data.dto;

public record AdminUpdateUserPermissionRequest(
        Boolean allowUpload,
        Boolean allowDownload
) {
}
