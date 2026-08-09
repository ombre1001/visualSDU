package cn.sduonline.business.data.vo;

public record MediaDownloadVO(
        Long mediaId,
        String downloadUrl,
        int expiresInSeconds
) {
}
