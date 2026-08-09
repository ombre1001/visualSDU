package cn.sduonline.business.data.vo;

public record SubmissionAssetVO(
        Long id,
        String originalName,
        String contentType,
        Long sizeBytes,
        Integer sortOrder,
        Long mediaId,
        String previewUrl
) {
}
