package cn.sduonline.business.data.dto;

import lombok.Builder;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Builder
public record StreamDownloadFile(
        InputStream inputStream,
        String contentType,
        Long size,
        String filename
) {
    public String encodedFilename() {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
