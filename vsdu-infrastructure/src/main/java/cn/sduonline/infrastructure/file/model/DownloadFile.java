package cn.sduonline.infrastructure.file.model;

import lombok.Builder;

import java.io.InputStream;

@Builder
public record DownloadFile(
        InputStream inputStream,
        String contentType,
        Long size
) {
}