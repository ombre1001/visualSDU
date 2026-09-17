package cn.sduonline.infrastructure.cdn;

import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@RequiredArgsConstructor
public class AliyunCdnUrlSigner {

    private final String domain;
    private final String privateKey;

    public String sign(String objectKey) {
        String path = objectKey.startsWith("/")
                ? objectKey
                : "/" + objectKey;

        long timestamp = Instant.now().getEpochSecond();

        String rand = UUID.randomUUID()
                .toString()
                .replace("-", "");

        String uid = "0";

        String signText = "%s-%d-%s-%s-%s".formatted(
                path,
                timestamp,
                rand,
                uid,
                privateKey
        );

        String md5 = md5(signText);

        String authKey = "%d-%s-%s-%s".formatted(
                timestamp,
                rand,
                uid,
                md5
        );

        return domain + path + "?auth_key=" + authKey;
    }

    private String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            byte[] hash = digest.digest(
                    text.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("生成CDN签名失败", e);
        }
    }
}